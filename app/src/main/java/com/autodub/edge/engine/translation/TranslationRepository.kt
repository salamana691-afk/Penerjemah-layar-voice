package com.autodub.edge.engine.translation

import android.util.LruCache
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.util.AppError
import com.autodub.edge.core.util.AppResult
import com.autodub.edge.domain.model.TranslationModelState
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface TranslationRepository {
    val modelState: StateFlow<TranslationModelState>
    suspend fun checkModelAvailability(sourceCode: String, targetCode: String): Boolean
    suspend fun ensureModelDownloaded(sourceCode: String, targetCode: String): AppResult<Unit>
    suspend fun translate(text: String, sourceCode: String, targetCode: String): AppResult<String>
    fun clearCache()
    fun close()
}

class MlKitTranslationRepository : TranslationRepository {

    private val _modelState = MutableStateFlow<TranslationModelState>(TranslationModelState.Unknown)
    override val modelState: StateFlow<TranslationModelState> = _modelState.asStateFlow()

    private val mutex = Mutex()
    private val translationCache = LruCache<String, String>(AppConstants.MAX_TRANSLATION_CACHE_SIZE)

    private var activeTranslator: Translator? = null
    private var activePairKey: String = ""
    private var isModelVerifiedReady: Boolean = false

    private fun resolveLanguage(code: String, fallback: String): String {
        return TranslateLanguage.fromLanguageTag(code.lowercase()) ?: fallback
    }

    private fun getOrCreateTranslator(sourceCode: String, targetCode: String): Pair<Translator, String> {
        val src = resolveLanguage(sourceCode, TranslateLanguage.ENGLISH)
        val dst = resolveLanguage(targetCode, TranslateLanguage.INDONESIAN)
        val pairKey = "$src->$dst"

        if (activeTranslator != null && activePairKey == pairKey) {
            return activeTranslator!! to pairKey
        }

        activeTranslator?.close()
        isModelVerifiedReady = false

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(src)
            .setTargetLanguage(dst)
            .build()
        val translator = Translation.getClient(options)
        activeTranslator = translator
        activePairKey = pairKey
        return translator to pairKey
    }

    override suspend fun checkModelAvailability(sourceCode: String, targetCode: String): Boolean {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    _modelState.value = TranslationModelState.Checking
                    val src = resolveLanguage(sourceCode, TranslateLanguage.ENGLISH)
                    val dst = resolveLanguage(targetCode, TranslateLanguage.INDONESIAN)
                    val modelManager = RemoteModelManager.getInstance()

                    val sourceReady = modelManager.isModelDownloaded(
                        TranslateRemoteModel.Builder(src).build()
                    ).await()
                    val targetReady = modelManager.isModelDownloaded(
                        TranslateRemoteModel.Builder(dst).build()
                    ).await()

                    val ready = sourceReady && targetReady
                    isModelVerifiedReady = ready
                    _modelState.value = if (ready) {
                        TranslationModelState.Ready
                    } else {
                        TranslationModelState.Unknown
                    }
                    ready
                } catch (t: Throwable) {
                    isModelVerifiedReady = false
                    _modelState.value = TranslationModelState.Error(
                        t.message ?: "Gagal memeriksa status model offline."
                    )
                    false
                }
            }
        }
    }

    override suspend fun ensureModelDownloaded(
        sourceCode: String,
        targetCode: String
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val (translator, pairKey) = getOrCreateTranslator(sourceCode, targetCode)
            try {
                _modelState.value = TranslationModelState.Downloading(
                    "Mengunduh model terjemahan offline ($pairKey)..."
                )
                val conditions = DownloadConditions.Builder().build()
                translator.downloadModelIfNeeded(conditions).await()
                isModelVerifiedReady = true
                _modelState.value = TranslationModelState.Ready
                AppResult.Success(Unit)
            } catch (t: Throwable) {
                isModelVerifiedReady = false
                val msg = t.message ?: "Gagal mengunduh model terjemahan offline."
                _modelState.value = TranslationModelState.Error(msg)
                AppResult.Error(AppError.TranslationModelUnavailable(pairKey, t))
            }
        }
    }

    override suspend fun translate(
        text: String,
        sourceCode: String,
        targetCode: String
    ): AppResult<String> = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return@withContext AppResult.Error(AppError.DialogNotFound())
        }

        val src = resolveLanguage(sourceCode, TranslateLanguage.ENGLISH)
        val dst = resolveLanguage(targetCode, TranslateLanguage.INDONESIAN)
        val pairKey = "$src->$dst"
        val cacheKey = "$pairKey::$trimmed"

        translationCache.get(cacheKey)?.let { cached ->
            return@withContext AppResult.Success(cached)
        }

        mutex.withLock {
            val (translator, _) = getOrCreateTranslator(sourceCode, targetCode)

            if (!isModelVerifiedReady) {
                val modelManager = RemoteModelManager.getInstance()
                val sourceReady = runCatching {
                    modelManager.isModelDownloaded(TranslateRemoteModel.Builder(src).build()).await()
                }.getOrDefault(false)
                val targetReady = runCatching {
                    modelManager.isModelDownloaded(TranslateRemoteModel.Builder(dst).build()).await()
                }.getOrDefault(false)

                if (!sourceReady || !targetReady) {
                    _modelState.value = TranslationModelState.Downloading(
                        "Menyiapkan model terjemahan ($pairKey)..."
                    )
                    try {
                        translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                    } catch (t: Throwable) {
                        _modelState.value = TranslationModelState.Error(
                            "Model terjemahan offline ($pairKey) belum tersedia."
                        )
                        return@withContext AppResult.Error(
                            AppError.TranslationModelUnavailable(pairKey, t)
                        )
                    }
                }
                isModelVerifiedReady = true
                _modelState.value = TranslationModelState.Ready
            }

            return@withContext try {
                val translated = translator.translate(trimmed).await().trim()
                if (translated.isNotEmpty()) {
                    translationCache.put(cacheKey, translated)
                }
                AppResult.Success(translated)
            } catch (t: Throwable) {
                AppResult.Error(AppError.TranslationModelUnavailable(pairKey, t))
            }
        }
    }

    override fun clearCache() {
        translationCache.evictAll()
    }

    override fun close() {
        activeTranslator?.close()
        activeTranslator = null
        activePairKey = ""
        isModelVerifiedReady = false
    }
}

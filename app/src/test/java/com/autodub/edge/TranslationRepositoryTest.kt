package com.autodub.edge

import com.autodub.edge.core.util.AppError
import com.autodub.edge.core.util.AppResult
import com.autodub.edge.domain.model.TranslationModelState
import com.autodub.edge.engine.translation.TranslationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeOfflineTranslationRepository : TranslationRepository {
    private val _state = MutableStateFlow<TranslationModelState>(TranslationModelState.Unknown)
    override val modelState: StateFlow<TranslationModelState> = _state

    var modelDownloaded = false
    private val cache = mutableMapOf<String, String>()
    var translateCalls = 0

    override suspend fun checkModelAvailability(sourceCode: String, targetCode: String): Boolean {
        _state.value = if (modelDownloaded) TranslationModelState.Ready else TranslationModelState.Unknown
        return modelDownloaded
    }

    override suspend fun ensureModelDownloaded(sourceCode: String, targetCode: String): AppResult<Unit> {
        modelDownloaded = true
        _state.value = TranslationModelState.Ready
        return AppResult.Success(Unit)
    }

    override suspend fun translate(text: String, sourceCode: String, targetCode: String): AppResult<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return AppResult.Error(AppError.DialogNotFound())
        if (!modelDownloaded) return AppResult.Error(AppError.TranslationModelUnavailable("$sourceCode->$targetCode"))
        val key = "$sourceCode->$targetCode::$trimmed"
        cache[key]?.let { return AppResult.Success(it) }
        translateCalls++
        val translated = "[ID] $trimmed"
        cache[key] = translated
        return AppResult.Success(translated)
    }

    override fun clearCache() {
        cache.clear()
    }

    override fun close() {}
}

class TranslationRepositoryTest {

    @Test
    fun `translation fails cleanly before model download and succeeds offline with caching after`() = runTest {
        val repo = FakeOfflineTranslationRepository()
        val before = repo.translate("Hello world", "en", "id")
        assertTrue(before is AppResult.Error)

        repo.ensureModelDownloaded("en", "id")
        val first = repo.translate("Hello world", "en", "id")
        val second = repo.translate("Hello world", "en", "id")

        assertTrue(first is AppResult.Success)
        assertEquals("[ID] Hello world", (first as AppResult.Success).data)
        assertEquals("[ID] Hello world", (second as AppResult.Success).data)
        assertEquals(1, repo.translateCalls)
    }
}

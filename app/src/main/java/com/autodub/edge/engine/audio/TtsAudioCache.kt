package com.autodub.edge.engine.audio

import com.autodub.edge.core.constants.AppConstants
import java.io.File
import java.security.MessageDigest
import java.util.Locale

class TtsAudioCache(
    private val cacheRootDir: File,
    private val maxBytes: Long = AppConstants.TTS_CACHE_MAX_BYTES
) {
    init {
        if (!cacheRootDir.exists()) {
            cacheRootDir.mkdirs()
        }
    }

    fun buildKey(
        providerId: String,
        modelId: String,
        voiceId: String,
        personalityId: String,
        emotionId: String,
        customDescription: String,
        pitch: Float,
        speechRate: Float,
        text: String
    ): String {
        val raw = listOf(
            providerId.trim(),
            modelId.trim(),
            voiceId.trim(),
            personalityId.trim(),
            emotionId.trim(),
            customDescription.trim(),
            String.format(Locale.US, "%.2f", pitch),
            String.format(Locale.US, "%.2f", speechRate),
            text.trim()
        ).joinToString("|")

        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    @Synchronized
    fun get(key: String, extension: String): File? {
        val file = File(cacheRootDir, "$key.$extension")
        if (file.exists() && file.length() > 44L) {
            file.setLastModified(System.currentTimeMillis())
            return file
        }
        return null
    }

    @Synchronized
    fun put(key: String, extension: String, sourceFile: File): File {
        if (!cacheRootDir.exists()) {
            cacheRootDir.mkdirs()
        }
        val target = File(cacheRootDir, "$key.$extension")
        if (sourceFile.absolutePath != target.absolutePath) {
            sourceFile.copyTo(target, overwrite = true)
        }
        target.setLastModified(System.currentTimeMillis())
        evictIfNeeded()
        return target
    }

    @Synchronized
    fun totalSizeBytes(): Long {
        return cacheRootDir.listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L
    }

    @Synchronized
    fun clear() {
        cacheRootDir.listFiles()?.forEach { file ->
            if (file.isFile) file.delete()
        }
    }

    private fun evictIfNeeded() {
        val files = cacheRootDir.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() }
            ?: return
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= maxBytes) break
            val len = file.length()
            if (file.delete()) {
                total -= len
            }
        }
    }
}

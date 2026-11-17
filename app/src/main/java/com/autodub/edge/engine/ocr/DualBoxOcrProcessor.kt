package com.autodub.edge.engine.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.autodub.edge.core.util.AppError
import com.autodub.edge.core.util.AppResult
import com.autodub.edge.core.util.TextNormalizer
import com.autodub.edge.domain.model.RoiBox
import com.autodub.edge.domain.model.RoiLayoutConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class DualOcrOutput(
    val rawCharacterName: String,
    val normalizedCharacterName: String,
    val rawDialogueText: String,
    val normalizedDialogueText: String,
    val processingTimeMs: Long
)

class DualBoxOcrProcessor {

    @Volatile
    private var recognizer: TextRecognizer? = null

    private fun getOrCreateRecognizer(): TextRecognizer {
        val current = recognizer
        if (current != null) return current
        return synchronized(this) {
            recognizer ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).also {
                recognizer = it
            }
        }
    }

    /**
     * Crops only the two configured ROI regions from [frameBitmap] and runs ML Kit OCR on each.
     * Caller remains responsible for recycling [frameBitmap] if it owns it.
     */
    suspend fun processFrame(
        frameBitmap: Bitmap,
        layout: RoiLayoutConfig,
        minConfidenceThreshold: Float = 0.0f
    ): AppResult<DualOcrOutput> = withContext(Dispatchers.Default) {
        if (frameBitmap.isRecycled || frameBitmap.width <= 4 || frameBitmap.height <= 4) {
            return@withContext AppResult.Error(AppError.OcrFailed("Frame gambar tidak valid."))
        }

        val startMs = System.currentTimeMillis()
        var nameCrop: Bitmap? = null
        var dialogCrop: Bitmap? = null

        try {
            val client = getOrCreateRecognizer()

            nameCrop = cropRoi(frameBitmap, layout.nameRoi)
            dialogCrop = cropRoi(frameBitmap, layout.dialogRoi)

            val dialogVisionText = if (dialogCrop != null) {
                client.process(InputImage.fromBitmap(dialogCrop, 0)).await()
            } else {
                null
            }

            val rawDialog = extractOrderedText(dialogVisionText, minConfidenceThreshold)
            val normalizedDialog = TextNormalizer.normalizeDialogueText(rawDialog)

            if (normalizedDialog.isBlank()) {
                return@withContext AppResult.Error(AppError.DialogNotFound())
            }

            val nameVisionText = if (nameCrop != null) {
                client.process(InputImage.fromBitmap(nameCrop, 0)).await()
            } else {
                null
            }

            val rawName = extractOrderedText(nameVisionText, minConfidenceThreshold)
            val normalizedName = TextNormalizer.normalizeCharacterName(rawName)

            AppResult.Success(
                DualOcrOutput(
                    rawCharacterName = rawName,
                    normalizedCharacterName = normalizedName,
                    rawDialogueText = rawDialog,
                    normalizedDialogueText = normalizedDialog,
                    processingTimeMs = System.currentTimeMillis() - startMs
                )
            )
        } catch (t: Throwable) {
            AppResult.Error(AppError.OcrFailed(t.message ?: "Kesalahan ML Kit OCR", t))
        } finally {
            nameCrop?.let { if (!it.isRecycled) it.recycle() }
            dialogCrop?.let { if (!it.isRecycled) it.recycle() }
        }
    }

    private fun cropRoi(source: Bitmap, roi: RoiBox): Bitmap? {
        val clamped = roi.clamped()
        val rect = Rect(
            (clamped.leftFraction * source.width).roundToInt().coerceIn(0, source.width - 1),
            (clamped.topFraction * source.height).roundToInt().coerceIn(0, source.height - 1),
            ((clamped.leftFraction + clamped.widthFraction) * source.width).roundToInt()
                .coerceIn(1, source.width),
            ((clamped.topFraction + clamped.heightFraction) * source.height).roundToInt()
                .coerceIn(1, source.height)
        )
        val w = rect.width()
        val h = rect.height()
        if (w < 8 || h < 8) return null
        return Bitmap.createBitmap(source, rect.left, rect.top, w, h)
    }

    private fun extractOrderedText(visionText: Text?, minConfidence: Float): String {
        if (visionText == null || visionText.textBlocks.isEmpty()) return ""
        val sortedBlocks = visionText.textBlocks.sortedWith(
            compareBy<Text.TextBlock> { it.boundingBox?.top ?: 0 }
                .thenBy { it.boundingBox?.left ?: 0 }
        )
        val linesOut = mutableListOf<String>()
        for (block in sortedBlocks) {
            val sortedLines = block.lines.sortedBy { it.boundingBox?.top ?: 0 }
            for (line in sortedLines) {
                val confidence = line.confidence
                if (confidence != null && confidence < minConfidence) continue
                val text = line.text.trim()
                if (text.isNotEmpty()) {
                    linesOut.add(text)
                }
            }
        }
        return linesOut.joinToString("\n")
    }

    fun close() {
        synchronized(this) {
            recognizer?.close()
            recognizer = null
        }
    }
}

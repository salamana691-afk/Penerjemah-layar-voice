package com.autodub.edge.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodub.edge.domain.model.OverlayAppearanceConfig

@Composable
fun TranslationOverlay(
    speakerName: String,
    sourceText: String,
    translatedText: String,
    errorMessage: String?,
    config: OverlayAppearanceConfig,
    onDrag: (dx: Float, dy: Float) -> Unit
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.coerceAtLeast(320)
    val boxWidthDp = (screenWidthDp * config.widthFraction).dp
    val bgAlpha = config.backgroundOpacity.coerceIn(0.1f, 0.98f)
    val containerColor = Color(0xFF0A0F1E).copy(alpha = bgAlpha)
    val textColor = Color(config.textColorArgb)

    Column(
        modifier = Modifier
            .width(boxWidthDp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(1.dp, Color(0x6600E5FF), RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = speakerName.ifBlank { "Narrator" },
                color = Color(0xFF00E5FF),
                fontSize = (config.fontSizeSp - 2f).coerceAtLeast(11f).sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Geser Overlay Terjemahan",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = Color(0xFFFCA5A5),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        } else if (translatedText.isBlank()) {
            Text(
                text = "Menunggu dialog game terdeteksi...",
                color = Color(0xFF94A3B8),
                fontSize = (config.fontSizeSp - 1f).coerceAtLeast(12f).sp
            )
        } else {
            Text(
                text = translatedText,
                color = textColor,
                fontSize = config.fontSizeSp.sp,
                lineHeight = (config.fontSizeSp * 1.38f).sp,
                fontWeight = FontWeight.Medium
            )
            if (config.showOriginalText && sourceText.isNotBlank()) {
                Text(
                    text = sourceText,
                    color = Color(0xFF94A3B8),
                    fontSize = (config.fontSizeSp - 3f).coerceAtLeast(11f).sp,
                    lineHeight = (config.fontSizeSp * 1.15f).sp
                )
            }
        }
    }
}

package com.autodub.edge.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.autodub.edge.domain.model.RoiMode

@Composable
fun FloatingControlBar(
    isPaused: Boolean,
    isTranslationVisible: Boolean,
    isVoiceEnabled: Boolean,
    roiMode: RoiMode,
    onTogglePause: () -> Unit,
    onToggleTranslationVisible: () -> Unit,
    onToggleVoice: () -> Unit,
    onToggleRoiSetup: () -> Unit,
    onOpenSettings: () -> Unit,
    onStopAll: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit
) {
    val barBg = Color(0xE60B1120)
    val borderColor = Color(0x8800E5FF)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(barBg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Geser Control Bar",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onTogglePause,
            modifier = Modifier.size(34.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (isPaused) Color(0xFF00E5FF) else Color(0xFFFACC15)
            )
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "Lanjutkan Terjemahan" else "Jeda Terjemahan",
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onToggleTranslationVisible,
            modifier = Modifier.size(34.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (isTranslationVisible) Color(0xFF00E5FF) else Color(0xFF64748B)
            )
        ) {
            Icon(
                imageVector = if (isTranslationVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Tampilkan/Sembunyikan Terjemahan",
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onToggleVoice,
            modifier = Modifier.size(34.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (isVoiceEnabled) Color(0xFFA855F7) else Color(0xFF64748B)
            )
        ) {
            Icon(
                imageVector = if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = "Aktifkan/Matikan Suara",
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onToggleRoiSetup,
            modifier = Modifier.size(34.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (roiMode == RoiMode.SETUP) Color(0xFF22C55E) else Color(0xFFCBD5E1)
            )
        ) {
            Icon(
                imageVector = Icons.Default.CropFree,
                contentDescription = "Atur Kotak ROI",
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(34.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFFE2E8F0))
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Buka Pengaturan",
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onStopAll,
            modifier = Modifier.size(34.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFFEF4444))
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Hentikan Layanan",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

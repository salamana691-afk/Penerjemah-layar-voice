package com.autodub.edge.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodub.edge.domain.model.RoiBox
import com.autodub.edge.domain.model.RoiLayoutConfig
import kotlin.math.roundToInt

@Composable
fun RoiOverlay(
    initialLayout: RoiLayoutConfig,
    onSaveAndScan: (RoiLayoutConfig) -> Unit,
    onResetDefault: () -> Unit
) {
    var nameBox by remember(initialLayout) { mutableStateOf(initialLayout.nameRoi) }
    var dialogBox by remember(initialLayout) { mutableStateOf(initialLayout.dialogRoi) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x33000000))
    ) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val containerHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        // ROI 1 — Character Name Detector
        DraggableResizableRoiBox(
            label = "ROI 1: Nama Karakter",
            roi = nameBox,
            containerWidthPx = containerWidthPx,
            containerHeightPx = containerHeightPx,
            onUpdate = { nameBox = it }
        )

        // ROI 2 — Dialog Detector
        DraggableResizableRoiBox(
            label = "ROI 2: Dialog Game",
            roi = dialogBox,
            containerWidthPx = containerWidthPx,
            containerHeightPx = containerHeightPx,
            onUpdate = { dialogBox = it }
        )

        // Top Setup Control Strip
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xEE0A0F1E))
                .border(1.dp, Color(0x8800E5FF), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Setup ROI",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedButton(
                onClick = onResetDefault
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(text = "Default", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
            }

            Button(
                onClick = {
                    onSaveAndScan(
                        initialLayout.copy(
                            nameRoi = nameBox.clamped(),
                            dialogRoi = dialogBox.clamped()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color(0xFF0A0F1E)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Simpan & Scan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DraggableResizableRoiBox(
    label: String,
    roi: RoiBox,
    containerWidthPx: Float,
    containerHeightPx: Float,
    onUpdate: (RoiBox) -> Unit
) {
    val density = LocalDensity.current
    val xPx = (roi.leftFraction * containerWidthPx).roundToInt()
    val yPx = (roi.topFraction * containerHeightPx).roundToInt()
    val wDp = with(density) { (roi.widthFraction * containerWidthPx).toDp() }
    val hDp = with(density) { (roi.heightFraction * containerHeightPx).toDp() }

    val borderColor = Color(roi.borderColorArgb)
    val fillColor = borderColor.copy(alpha = roi.fillAlpha.coerceIn(0.05f, 0.7f))

    Box(
        modifier = Modifier
            .offset { IntOffset(xPx, yPx) }
            .size(width = wDp, height = hDp)
            .background(fillColor, RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .pointerInput(roi) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dxFrac = dragAmount.x / containerWidthPx
                    val dyFrac = dragAmount.y / containerHeightPx
                    onUpdate(
                        roi.copy(
                            leftFraction = roi.leftFraction + dxFrac,
                            topFraction = roi.topFraction + dyFrac
                        ).clamped()
                    )
                }
            }
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Color(0xBB0A0F1E), RoundedCornerShape(bottomEnd = 6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )

        // Bottom-Right Resize Handle
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(borderColor)
                .pointerInput(roi) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dwFrac = dragAmount.x / containerWidthPx
                        val dhFrac = dragAmount.y / containerHeightPx
                        onUpdate(
                            roi.copy(
                                widthFraction = (roi.widthFraction + dwFrac).coerceIn(0.08f, 0.96f),
                                heightFraction = (roi.heightFraction + dhFrac).coerceIn(0.05f, 0.65f)
                            ).clamped()
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.OpenWith,
                contentDescription = "Ubah Ukuran $label",
                tint = Color(0xFF0A0F1E),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

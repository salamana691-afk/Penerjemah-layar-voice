package com.autodub.edge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodub.edge.domain.model.RoiLayoutConfig
import com.autodub.edge.domain.model.RoiMode
import com.autodub.edge.ui.components.CyberCard
import com.autodub.edge.ui.components.LabeledSlider
import com.autodub.edge.ui.theme.CyberColors
import kotlin.math.roundToInt

@Composable
fun RoiConfigScreen(
    roiLayout: RoiLayoutConfig,
    onSaveLayout: (RoiLayoutConfig) -> Unit,
    onResetDefault: () -> Unit,
    onToggleOverlaySetupMode: (RoiMode) -> Unit
) {
    var draft by remember(roiLayout) { mutableStateOf(roiLayout) }
    var editingNameBox by remember { mutableStateOf(true) }

    val activeRoi = if (editingNameBox) draft.nameRoi else draft.dialogRoi

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Konfigurasi Dual-Box ROI",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CyberColors.TextPrimary
        )

        CyberCard {
            Text(
                text = "Pratinjau Layar Game (16:9)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF070B14))
                    .border(1.dp, CyberColors.BorderSlate, RoundedCornerShape(10.dp))
            ) {
                val density = LocalDensity.current
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }

                val n = draft.nameRoi.clamped()
                val d = draft.dialogRoi.clamped()

                // ROI 1 Preview
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (n.leftFraction * wPx).roundToInt(),
                                (n.topFraction * hPx).roundToInt()
                            )
                        }
                        .size(
                            width = with(density) { (n.widthFraction * wPx).toDp() },
                            height = with(density) { (n.heightFraction * hPx).toDp() }
                        )
                        .background(Color(n.borderColorArgb).copy(alpha = n.fillAlpha), RoundedCornerShape(6.dp))
                        .border(2.dp, Color(n.borderColorArgb), RoundedCornerShape(6.dp))
                        .padding(4.dp)
                ) {
                    Text("ROI 1: Nama", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // ROI 2 Preview
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (d.leftFraction * wPx).roundToInt(),
                                (d.topFraction * hPx).roundToInt()
                            )
                        }
                        .size(
                            width = with(density) { (d.widthFraction * wPx).toDp() },
                            height = with(density) { (d.heightFraction * hPx).toDp() }
                        )
                        .background(Color(d.borderColorArgb).copy(alpha = d.fillAlpha), RoundedCornerShape(6.dp))
                        .border(2.dp, Color(d.borderColorArgb), RoundedCornerShape(6.dp))
                        .padding(6.dp)
                ) {
                    Text("ROI 2: Dialog Visual Novel", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { editingNameBox = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (editingNameBox) CyberColors.CyberCyan else CyberColors.SurfaceNavy,
                        contentColor = if (editingNameBox) CyberColors.DarkNavy else CyberColors.TextPrimary
                    )
                ) {
                    Text("Edit ROI 1 (Nama)", fontSize = 12.sp)
                }
                Button(
                    onClick = { editingNameBox = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!editingNameBox) CyberColors.CyberPurple else CyberColors.SurfaceNavy,
                        contentColor = Color.White
                    )
                ) {
                    Text("Edit ROI 2 (Dialog)", fontSize = 12.sp)
                }
            }
        }

        CyberCard {
            Text(
                text = if (editingNameBox) "Pengaturan Kotak Nama Karakter" else "Pengaturan Kotak Dialog",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )

            LabeledSlider(
                label = "Posisi Horizontal (X)",
                value = activeRoi.leftFraction,
                valueRange = 0f..0.85f,
                onValueChange = { v ->
                    val updated = activeRoi.copy(leftFraction = v).clamped()
                    draft = if (editingNameBox) draft.copy(nameRoi = updated) else draft.copy(dialogRoi = updated)
                }
            )
            LabeledSlider(
                label = "Posisi Vertikal (Y)",
                value = activeRoi.topFraction,
                valueRange = 0f..0.90f,
                onValueChange = { v ->
                    val updated = activeRoi.copy(topFraction = v).clamped()
                    draft = if (editingNameBox) draft.copy(nameRoi = updated) else draft.copy(dialogRoi = updated)
                }
            )
            LabeledSlider(
                label = "Lebar Kotak",
                value = activeRoi.widthFraction,
                valueRange = 0.08f..0.96f,
                onValueChange = { v ->
                    val updated = activeRoi.copy(widthFraction = v).clamped()
                    draft = if (editingNameBox) draft.copy(nameRoi = updated) else draft.copy(dialogRoi = updated)
                }
            )
            LabeledSlider(
                label = "Tinggi Kotak",
                value = activeRoi.heightFraction,
                valueRange = 0.05f..0.60f,
                onValueChange = { v ->
                    val updated = activeRoi.copy(heightFraction = v).clamped()
                    draft = if (editingNameBox) draft.copy(nameRoi = updated) else draft.copy(dialogRoi = updated)
                }
            )
            LabeledSlider(
                label = "Transparansi Isi",
                value = activeRoi.fillAlpha,
                valueRange = 0.05f..0.80f,
                onValueChange = { v ->
                    val updated = activeRoi.copy(fillAlpha = v).clamped()
                    draft = if (editingNameBox) draft.copy(nameRoi = updated) else draft.copy(dialogRoi = updated)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Warna Garis:", fontSize = 13.sp, color = CyberColors.TextSecondary)
                val colors = listOf(0xFF00E5FF.toInt(), 0xFFA855F7.toInt(), 0xFF22C55E.toInt(), 0xFFFACC15.toInt())
                colors.forEach { argb ->
                    OutlinedButton(
                        onClick = {
                            val updated = activeRoi.copy(borderColorArgb = argb)
                            draft = if (editingNameBox) draft.copy(nameRoi = updated) else draft.copy(dialogRoi = updated)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color(argb), RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onResetDefault,
                modifier = Modifier.weight(1f)
            ) {
                Text("Pulihkan Default")
            }
            Button(
                onClick = { onSaveLayout(draft) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberColors.CyberCyan,
                    contentColor = CyberColors.DarkNavy
                )
            ) {
                Text("Save Layout", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(
            onClick = {
                val nextMode = if (draft.mode == RoiMode.SETUP) RoiMode.SCAN else RoiMode.SETUP
                draft = draft.copy(mode = nextMode)
                onToggleOverlaySetupMode(nextMode)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (draft.mode == RoiMode.SETUP) "Mode Saat Ini: Setup Mode (Klik untuk Scan Mode)"
                else "Mode Saat Ini: Scan Mode (Klik untuk Setup Mode di Game)"
            )
        }
    }
}

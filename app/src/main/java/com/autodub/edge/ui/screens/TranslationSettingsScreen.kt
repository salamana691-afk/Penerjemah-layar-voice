package com.autodub.edge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodub.edge.domain.model.DubbingSettings
import com.autodub.edge.domain.model.OverlayAppearanceConfig
import com.autodub.edge.domain.model.TranslationModelState
import com.autodub.edge.ui.components.CyberCard
import com.autodub.edge.ui.components.LabeledSlider
import com.autodub.edge.ui.components.StatusLine
import com.autodub.edge.ui.theme.CyberColors

@Composable
fun TranslationSettingsScreen(
    dubbingSettings: DubbingSettings,
    overlayConfig: OverlayAppearanceConfig,
    modelState: TranslationModelState,
    onSaveDubbingSettings: (DubbingSettings) -> Unit,
    onSaveOverlayConfig: (OverlayAppearanceConfig) -> Unit,
    onDownloadOfflineModel: () -> Unit
) {
    var draftSettings by remember(dubbingSettings) { mutableStateOf(dubbingSettings) }
    var draftOverlay by remember(overlayConfig) { mutableStateOf(overlayConfig) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Translation & Overlay Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CyberColors.TextPrimary
        )

        CyberCard {
            Text(
                text = "Bahasa & Model Offline (ML Kit)",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )

            StatusLine(
                label = "Source Language",
                value = "English (${draftSettings.sourceLanguageCode})",
                valueColor = CyberColors.CyberCyan
            )
            StatusLine(
                label = "Target Language",
                value = "Indonesian (${draftSettings.targetLanguageCode})",
                valueColor = CyberColors.CyberPurple
            )

            val modelStatusText = when (modelState) {
                TranslationModelState.Ready -> "Model Offline Siap Digunakan"
                TranslationModelState.Checking -> "Memeriksa Model..."
                is TranslationModelState.Downloading -> modelState.progressMessage
                is TranslationModelState.Error -> modelState.message
                TranslationModelState.Unknown -> "Model Belum Diunduh"
            }
            Text(text = modelStatusText, fontSize = 12.sp, color = CyberColors.TextSecondary)

            OutlinedButton(
                onClick = onDownloadOfflineModel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Text("Unduh / Verifikasi Model Offline", modifier = Modifier.padding(start = 8.dp))
            }
        }

        CyberCard {
            Text(
                text = "OCR & Pipeline Optimization",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )

            LabeledSlider(
                label = "OCR Sensitivity",
                value = draftSettings.ocrSensitivity,
                valueRange = 0.4f..0.95f,
                valueLabel = "${(draftSettings.ocrSensitivity * 100).toInt()}%",
                onValueChange = { draftSettings = draftSettings.copy(ocrSensitivity = it) }
            )

            LabeledSlider(
                label = "Frame Scan Interval (ms)",
                value = draftSettings.ocrIntervalMs.toFloat(),
                valueRange = 300f..2000f,
                valueLabel = "${draftSettings.ocrIntervalMs} ms",
                onValueChange = { draftSettings = draftSettings.copy(ocrIntervalMs = it.toLong()) }
            )

            LabeledSlider(
                label = "Translation Delay / Debounce (ms)",
                value = draftSettings.translationDelayMs.toFloat(),
                valueRange = 0f..1000f,
                valueLabel = "${draftSettings.translationDelayMs} ms",
                onValueChange = { draftSettings = draftSettings.copy(translationDelayMs = it.toLong()) }
            )
        }

        CyberCard {
            Text(
                text = "Tampilan Floating Overlay",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )

            LabeledSlider(
                label = "Overlay Font Size",
                value = draftOverlay.fontSizeSp,
                valueRange = 12f..28f,
                valueLabel = "${draftOverlay.fontSizeSp.toInt()} sp",
                onValueChange = { draftOverlay = draftOverlay.copy(fontSizeSp = it) }
            )

            LabeledSlider(
                label = "Overlay Opacity",
                value = draftOverlay.backgroundOpacity,
                valueRange = 0.15f..0.98f,
                valueLabel = "${(draftOverlay.backgroundOpacity * 100).toInt()}%",
                onValueChange = { draftOverlay = draftOverlay.copy(backgroundOpacity = it) }
            )

            LabeledSlider(
                label = "Lebar Kotak Overlay",
                value = draftOverlay.widthFraction,
                valueRange = 0.45f..0.96f,
                valueLabel = "${(draftOverlay.widthFraction * 100).toInt()}%",
                onValueChange = { draftOverlay = draftOverlay.copy(widthFraction = it) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Warna Teks:", fontSize = 13.sp, color = CyberColors.TextSecondary)
                listOf(0xFFFFFFFF.toInt(), 0xFF00E5FF.toInt(), 0xFFFDE047.toInt(), 0xFFE2E8F0.toInt()).forEach { argb ->
                    OutlinedButton(
                        onClick = { draftOverlay = draftOverlay.copy(textColorArgb = argb) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color(argb), RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tampilkan Teks Asli (EN)", color = CyberColors.TextPrimary, fontSize = 13.sp)
                Switch(
                    checked = draftOverlay.showOriginalText,
                    onCheckedChange = { draftOverlay = draftOverlay.copy(showOriginalText = it) }
                )
            }
        }

        Button(
            onClick = {
                onSaveDubbingSettings(draftSettings)
                onSaveOverlayConfig(draftOverlay)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberColors.CyberCyan,
                contentColor = CyberColors.DarkNavy
            )
        ) {
            Text("Simpan Pengaturan Terjemahan & Overlay", fontWeight = FontWeight.Bold)
        }
    }
}

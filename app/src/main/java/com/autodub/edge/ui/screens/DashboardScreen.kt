package com.autodub.edge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodub.edge.domain.model.CaptureStatus
import com.autodub.edge.domain.model.DialogueEvent
import com.autodub.edge.domain.model.OcrStatus
import com.autodub.edge.domain.model.PipelineRuntimeState
import com.autodub.edge.domain.model.TranslationModelState
import com.autodub.edge.domain.model.VoiceEngineStatus
import com.autodub.edge.ui.components.AutoDubLogo
import com.autodub.edge.ui.components.CyberCard
import com.autodub.edge.ui.components.StatusLine
import com.autodub.edge.ui.theme.CyberColors

@Composable
fun DashboardScreen(
    runtimeState: PipelineRuntimeState,
    recentDialogues: List<DialogueEvent>,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onStartTranslation: () -> Unit,
    onStopTranslation: () -> Unit,
    onDownloadModel: () -> Unit,
    onNavigateRoi: () -> Unit,
    onNavigateCharacters: () -> Unit,
    onNavigateVoiceSettings: () -> Unit,
    onNavigateTranslationSettings: () -> Unit,
    onNavigateAbout: () -> Unit
) {
    val isRunning = runtimeState.captureStatus == CaptureStatus.RUNNING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AutoDubLogo()

        if (!hasOverlayPermission || !hasNotificationPermission) {
            CyberCard(borderColor = CyberColors.AmberWarn) {
                Text(
                    text = "Izin Sistem Diperlukan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberColors.AmberWarn
                )
                if (!hasOverlayPermission) {
                    Text(
                        text = "Izin SYSTEM_ALERT_WINDOW diperlukan agar terjemahan dan kontrol tampil di atas game.",
                        fontSize = 12.sp,
                        color = CyberColors.TextSecondary
                    )
                    Button(
                        onClick = onRequestOverlayPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberColors.CyberCyan,
                            contentColor = CyberColors.DarkNavy
                        )
                    ) {
                        Text("Berikan Izin Overlay", fontWeight = FontWeight.Bold)
                    }
                }
                if (!hasNotificationPermission) {
                    OutlinedButton(onClick = onRequestNotificationPermission) {
                        Text("Berikan Izin Notifikasi")
                    }
                }
            }
        }

        CyberCard {
            Text(
                text = "Status Mesin Real-Time",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )

            StatusLine(
                label = "Screen Capture (MediaProjection)",
                value = when (runtimeState.captureStatus) {
                    CaptureStatus.RUNNING -> "Aktif"
                    CaptureStatus.PAUSED -> "Dijeda"
                    CaptureStatus.REQUESTING_PERMISSION -> "Meminta Izin"
                    CaptureStatus.ERROR -> "Error"
                    CaptureStatus.IDLE -> "Siap"
                },
                valueColor = when (runtimeState.captureStatus) {
                    CaptureStatus.RUNNING -> CyberColors.EmeraldActive
                    CaptureStatus.ERROR -> CyberColors.CrimsonError
                    CaptureStatus.PAUSED -> CyberColors.AmberWarn
                    else -> CyberColors.TextSecondary
                }
            )

            StatusLine(
                label = "Dual-Box OCR (ML Kit)",
                value = when (runtimeState.ocrStatus) {
                    OcrStatus.SCANNING -> "Memindai ROI"
                    OcrStatus.PAUSED -> "Dijeda"
                    OcrStatus.ERROR -> "Error"
                    OcrStatus.IDLE -> "Standby"
                },
                valueColor = when (runtimeState.ocrStatus) {
                    OcrStatus.SCANNING -> CyberColors.CyberCyan
                    OcrStatus.ERROR -> CyberColors.CrimsonError
                    else -> CyberColors.TextSecondary
                }
            )

            val (modelLabel, modelColor) = when (val m = runtimeState.translationModelState) {
                TranslationModelState.Ready -> "Model Offline Siap (EN → ID)" to CyberColors.EmeraldActive
                TranslationModelState.Checking -> "Memeriksa Model..." to CyberColors.AmberWarn
                is TranslationModelState.Downloading -> m.progressMessage to CyberColors.CyberCyan
                is TranslationModelState.Error -> "Gagal: ${m.message}" to CyberColors.CrimsonError
                TranslationModelState.Unknown -> "Belum Diunduh" to CyberColors.AmberWarn
            }
            StatusLine(
                label = "Translation Engine",
                value = modelLabel,
                valueColor = modelColor
            )

            StatusLine(
                label = "Koneksi & Dual Voice Mode",
                value = "${if (runtimeState.isNetworkOnline) "Online" else "Offline"} · ${runtimeState.activeVoiceProviderLabel}",
                valueColor = if (runtimeState.isNetworkOnline) CyberColors.CyberCyan else CyberColors.AmberWarn
            )

            StatusLine(
                label = "AI Voice Engine",
                value = when (runtimeState.voiceEngineStatus) {
                    VoiceEngineStatus.READY -> "Siap"
                    VoiceEngineStatus.SPEAKING -> "Berbicara (${runtimeState.queueSize} antrean)"
                    VoiceEngineStatus.FALLBACK_ACTIVE -> "Fallback Android TTS"
                    VoiceEngineStatus.ERROR -> "Error TTS"
                    VoiceEngineStatus.INITIALIZING -> "Inisialisasi..."
                },
                valueColor = when (runtimeState.voiceEngineStatus) {
                    VoiceEngineStatus.SPEAKING -> CyberColors.CyberPurple
                    VoiceEngineStatus.READY -> CyberColors.EmeraldActive
                    VoiceEngineStatus.FALLBACK_ACTIVE -> CyberColors.AmberWarn
                    VoiceEngineStatus.ERROR -> CyberColors.CrimsonError
                    VoiceEngineStatus.INITIALIZING -> CyberColors.TextSecondary
                }
            )

            if (runtimeState.translationModelState !is TranslationModelState.Ready) {
                OutlinedButton(
                    onClick = onDownloadModel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Text(
                        text = "Unduh Model Terjemahan Offline (EN → ID)",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (!runtimeState.lastErrorMessage.isNullOrBlank()) {
                Text(
                    text = runtimeState.lastErrorMessage,
                    fontSize = 12.sp,
                    color = CyberColors.CrimsonError
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onStartTranslation,
                enabled = !isRunning,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberColors.CyberCyan,
                    contentColor = CyberColors.DarkNavy
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(
                    text = "Start Translation",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Button(
                onClick = onStopTranslation,
                enabled = isRunning || runtimeState.isOverlayRunning,
                modifier = Modifier.weight(0.75f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberColors.CrimsonError,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Text(
                    text = "Stop",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        CyberCard {
            Text(
                text = "Menu Konfigurasi",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateRoi,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CropFree, contentDescription = null)
                    Text("ROI Setup", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onNavigateCharacters,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.People, contentDescription = null)
                    Text("Karakter", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateVoiceSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                    Text("Voice AI", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onNavigateTranslationSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Text("Terjemahan", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp)
                }
            }

            OutlinedButton(
                onClick = onNavigateAbout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Text("Tentang AutoDub Edge", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp)
            }
        }

        if (recentDialogues.isNotEmpty()) {
            CyberCard {
                Text(
                    text = "Riwayat Dialog Terakhir",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberColors.TextPrimary
                )
                recentDialogues.take(5).forEach { event ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${event.speakerName} · ${event.emotionUsed} · ${event.providerUsed}${if (event.cacheHit) " (Cache)" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberColors.CyberCyan
                        )
                        Text(
                            text = event.translatedText,
                            fontSize = 13.sp,
                            color = CyberColors.TextPrimary
                        )
                        Text(
                            text = event.sourceText,
                            fontSize = 11.sp,
                            color = CyberColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

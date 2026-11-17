package com.autodub.edge.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.domain.model.AudioQueuePolicy
import com.autodub.edge.domain.model.DubbingSettings
import com.autodub.edge.domain.model.VoiceDescriptor
import com.autodub.edge.domain.model.VoiceEmotion
import com.autodub.edge.domain.model.VoiceEngineOperatingMode
import com.autodub.edge.domain.model.VoicePersonality
import com.autodub.edge.domain.model.VoiceProviderType
import com.autodub.edge.ui.components.CyberCard
import com.autodub.edge.ui.components.LabeledSlider
import com.autodub.edge.ui.theme.CyberColors

@Composable
fun VoiceSettingsScreen(
    settings: DubbingSettings,
    availableVoices: List<VoiceDescriptor>,
    cacheSizeBytes: Long,
    onProviderChanged: (VoiceProviderType) -> Unit,
    onSaveSettings: (DubbingSettings) -> Unit,
    onClearAudioCache: () -> Unit,
    onTestVoice: () -> Unit,
    onSkipCurrent: () -> Unit
) {
    var draft by remember(settings) { mutableStateOf(settings) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Dual Voice Engine & AI Expressive Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CyberColors.TextPrimary
        )

        CyberCard {
            Text(
                text = "Dual Voice Switching Mode",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )

            VoiceEngineOperatingMode.entries.forEach { mode ->
                val selected = draft.voiceOperatingMode == mode
                OutlinedButton(
                    onClick = {
                        draft = draft.copy(voiceOperatingMode = mode)
                        onSaveSettings(draft)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (selected) "✓ ${mode.label}" else mode.label,
                            color = if (selected) CyberColors.CyberCyan else CyberColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = mode.description,
                            color = CyberColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        CyberCard {
            Text(
                text = "Voice Provider Default",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )

            VoiceProviderType.entries.forEach { provider ->
                val selected = draft.defaultProvider == provider
                Button(
                    onClick = {
                        draft = draft.copy(defaultProvider = provider)
                        onProviderChanged(provider)
                        onSaveSettings(draft)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) CyberColors.CyberCyan else CyberColors.SurfaceNavy,
                        contentColor = if (selected) CyberColors.DarkNavy else CyberColors.TextPrimary
                    )
                ) {
                    Text(provider.displayName, fontWeight = FontWeight.SemiBold)
                }
            }

            if (draft.defaultProvider == VoiceProviderType.GEMINI_TTS) {
                Text("Model Gemini Expressive TTS", fontSize = 12.sp, color = CyberColors.TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = draft.geminiModelId == AppConstants.DEFAULT_GEMINI_TTS_MODEL,
                        onClick = {
                            draft = draft.copy(geminiModelId = AppConstants.DEFAULT_GEMINI_TTS_MODEL)
                        },
                        label = { Text("gemini-3.8-flash-tts (Voice Design)", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = draft.geminiModelId == AppConstants.DEFAULT_GEMINI_LITE_TTS_MODEL,
                        onClick = {
                            draft = draft.copy(geminiModelId = AppConstants.DEFAULT_GEMINI_LITE_TTS_MODEL)
                        },
                        label = { Text("flash-lite-tts (Cepat)", fontSize = 10.sp) }
                    )
                }

                OutlinedTextField(
                    value = draft.geminiApiKey,
                    onValueChange = { draft = draft.copy(geminiApiKey = it) },
                    label = { Text("Gemini API Key (disimpan lokal di DataStore)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.geminiEndpointUrl,
                    onValueChange = { draft = draft.copy(geminiEndpointUrl = it) },
                    label = { Text("Atau URL Backend Proxy TTS Anda (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Dokumentasi Provider: API key TIDAK pernah di-hardcode di dalam APK. Dapatkan key resmi dari Google AI Studio. Free tier memiliki batas RPM/RPD tertentu; apabila kuota habis (HTTP 429) atau offline, sistem otomatis beralih ke Android Native TTS.",
                    fontSize = 11.sp,
                    color = CyberColors.TextSecondary
                )
            }

            if (draft.defaultProvider == VoiceProviderType.EDGE_COMPATIBLE) {
                OutlinedTextField(
                    value = draft.edgeEndpointUrl,
                    onValueChange = { draft = draft.copy(edgeEndpointUrl = it) },
                    label = { Text("Authorized WebSocket TTS Endpoint (wss://...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.edgeBearerToken,
                    onValueChange = { draft = draft.copy(edgeBearerToken = it) },
                    label = { Text("Bearer Token (opsional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        CyberCard {
            Text(
                text = "Default Persona, Emosi & Voice",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )

            Text("Anime-Inspired Personality", fontSize = 12.sp, color = CyberColors.TextSecondary)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VoicePersonality.entries.forEach { p ->
                    FilterChip(
                        selected = draft.defaultPersonality == p,
                        onClick = {
                            draft = draft.copy(
                                defaultPersonality = p,
                                defaultVoiceId = if (draft.defaultProvider == VoiceProviderType.GEMINI_TTS) {
                                    p.defaultGeminiVoice
                                } else {
                                    draft.defaultVoiceId
                                }
                            )
                        },
                        label = { Text(p.displayName, fontSize = 11.sp) }
                    )
                }
            }

            Text("Default Emotion", fontSize = 12.sp, color = CyberColors.TextSecondary)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VoiceEmotion.entries.forEach { em ->
                    FilterChip(
                        selected = draft.defaultEmotion == em,
                        onClick = { draft = draft.copy(defaultEmotion = em) },
                        label = { Text(em.displayName, fontSize = 11.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = draft.defaultCustomVoiceDescription,
                onValueChange = { draft = draft.copy(defaultCustomVoiceDescription = it) },
                label = { Text("Custom Voice Description Default") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = draft.defaultVoiceId,
                onValueChange = { draft = draft.copy(defaultVoiceId = it) },
                label = { Text("Voice ID Aktif") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (availableVoices.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableVoices.take(6).forEach { voice ->
                        OutlinedButton(
                            onClick = {
                                draft = draft.copy(defaultVoiceId = voice.id)
                                onSaveSettings(draft)
                            }
                        ) {
                            Text(voice.id, fontSize = 11.sp)
                        }
                    }
                }
            }

            LabeledSlider(
                label = "Default Pitch",
                value = draft.defaultPitch,
                valueRange = 0.5f..2.0f,
                onValueChange = { draft = draft.copy(defaultPitch = it) }
            )
            LabeledSlider(
                label = "Default Speech Rate",
                value = draft.defaultSpeechRate,
                valueRange = 0.5f..2.0f,
                onValueChange = { draft = draft.copy(defaultSpeechRate = it) }
            )
            LabeledSlider(
                label = "Speech Volume",
                value = draft.defaultVolume,
                valueRange = 0.0f..1.0f,
                valueLabel = "${(draft.defaultVolume * 100).toInt()}%",
                onValueChange = { draft = draft.copy(defaultVolume = it) }
            )
        }

        CyberCard {
            Text(
                text = "Audio Cache & Queue Policy",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Voice Enabled", color = CyberColors.TextPrimary, fontSize = 13.sp)
                Switch(
                    checked = draft.voiceEnabled,
                    onCheckedChange = {
                        draft = draft.copy(voiceEnabled = it)
                        onSaveSettings(draft)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto Play Terjemahan Baru", color = CyberColors.TextPrimary, fontSize = 13.sp)
                Switch(
                    checked = draft.autoPlay,
                    onCheckedChange = {
                        draft = draft.copy(autoPlay = it)
                        onSaveSettings(draft)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Simpan Audio Sementara (Cache)", color = CyberColors.TextPrimary, fontSize = 13.sp)
                    Text(
                        text = "Dialog yang sama tidak perlu di-generate ulang (${String.format("%.2f", cacheSizeBytes / (1024f * 1024f))} MB terpakai)",
                        color = CyberColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = draft.enableAudioCache,
                    onCheckedChange = {
                        draft = draft.copy(enableAudioCache = it)
                        onSaveSettings(draft)
                    }
                )
            }

            OutlinedButton(
                onClick = onClearAudioCache,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                Text("Bersihkan Cache Audio TTS", modifier = Modifier.padding(start = 6.dp))
            }

            AudioQueuePolicy.entries.forEach { policy ->
                val active = draft.audioQueuePolicy == policy
                OutlinedButton(
                    onClick = {
                        draft = draft.copy(audioQueuePolicy = policy)
                        onSaveSettings(draft)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (active) "✓ ${policy.label}" else policy.label,
                            color = if (active) CyberColors.CyberCyan else CyberColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = policy.description,
                            color = CyberColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    onSaveSettings(draft)
                    onTestVoice()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberColors.CyberCyan,
                    contentColor = CyberColors.DarkNavy
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("Test Voice", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }

            OutlinedButton(
                onClick = onSkipCurrent,
                modifier = Modifier.weight(0.9f)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = null)
                Text("Skip Dialogue", modifier = Modifier.padding(start = 6.dp))
            }
        }

        Button(
            onClick = { onSaveSettings(draft) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberColors.CyberPurple,
                contentColor = Color.White
            )
        ) {
            Text("Simpan Pengaturan Dual Voice", fontWeight = FontWeight.Bold)
        }
    }
}

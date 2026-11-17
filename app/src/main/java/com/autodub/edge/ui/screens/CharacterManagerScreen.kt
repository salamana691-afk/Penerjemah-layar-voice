package com.autodub.edge.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.domain.model.CharacterProfile
import com.autodub.edge.domain.model.VoiceDescriptor
import com.autodub.edge.domain.model.VoiceEmotion
import com.autodub.edge.domain.model.VoicePersonality
import com.autodub.edge.ui.components.CyberCard
import com.autodub.edge.ui.components.LabeledSlider
import com.autodub.edge.ui.theme.CyberColors

@Composable
fun CharacterManagerScreen(
    characters: List<CharacterProfile>,
    availableVoices: List<VoiceDescriptor>,
    offlineVoices: List<VoiceDescriptor>,
    statusMessage: String?,
    onLoadVoicesForProvider: (String) -> Unit,
    onSaveCharacter: (CharacterProfile) -> Unit,
    onDeleteCharacter: (Long) -> Unit,
    onTestVoice: (CharacterProfile) -> Unit,
    onBuildVoicePromptPreview: (CharacterProfile) -> String,
    onBuildSsmlPreview: (CharacterProfile, String) -> String
) {
    var editingProfile by remember { mutableStateOf<CharacterProfile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Character Voice Manager",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberColors.TextPrimary
                )
                Text(
                    text = "Persona suara anime, kontrol emosi & deskripsi kustom tersimpan di Room",
                    fontSize = 12.sp,
                    color = CyberColors.TextSecondary
                )
            }

            Button(
                onClick = {
                    onLoadVoicesForProvider(AppConstants.PROVIDER_GEMINI_TTS)
                    editingProfile = CharacterProfile(characterName = "")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberColors.CyberCyan,
                    contentColor = CyberColors.DarkNavy
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Tambah", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
            }
        }

        if (!statusMessage.isNullOrBlank()) {
            Text(text = statusMessage, fontSize = 12.sp, color = CyberColors.CyberCyan)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(characters, key = { it.id }) { profile ->
                val isDefaultNarrator = profile.characterName == AppConstants.DEFAULT_NARRATOR_NAME
                CyberCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (isDefaultNarrator) AppConstants.DEFAULT_NARRATOR_DISPLAY else profile.characterName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDefaultNarrator) CyberColors.CyberPurple else CyberColors.TextPrimary
                            )
                            Text(
                                text = "${profile.voicePersonality.displayName} · Emosi: ${profile.emotion.displayName}",
                                fontSize = 12.sp,
                                color = CyberColors.CyberCyan
                            )
                            Text(
                                text = "${profile.voiceProvider} (${profile.voiceId.ifBlank { "Default" }}) · Pitch ${String.format("%.2f", profile.pitch)}x · Rate ${String.format("%.2f", profile.speechRate)}x",
                                fontSize = 11.sp,
                                color = CyberColors.TextSecondary
                            )
                            if (profile.customVoiceDescription.isNotBlank()) {
                                Text(
                                    text = profile.customVoiceDescription,
                                    fontSize = 11.sp,
                                    color = CyberColors.TextSecondary,
                                    maxLines = 2
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onTestVoice(profile) }) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Test Voice",
                                    tint = CyberColors.EmeraldActive
                                )
                            }
                            IconButton(onClick = {
                                onLoadVoicesForProvider(profile.voiceProvider)
                                editingProfile = profile
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Karakter",
                                    tint = CyberColors.CyberCyan
                                )
                            }
                            if (!isDefaultNarrator) {
                                IconButton(onClick = { onDeleteCharacter(profile.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus Karakter",
                                        tint = CyberColors.CrimsonError
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingProfile?.let { initial ->
        CharacterEditDialog(
            initial = initial,
            availableVoices = availableVoices,
            offlineVoices = offlineVoices,
            onProviderChanged = onLoadVoicesForProvider,
            onDismiss = { editingProfile = null },
            onTest = onTestVoice,
            onSave = { updated ->
                onSaveCharacter(updated)
                editingProfile = null
            },
            onBuildVoicePromptPreview = onBuildVoicePromptPreview,
            onBuildSsmlPreview = onBuildSsmlPreview
        )
    }
}

@Composable
private fun CharacterEditDialog(
    initial: CharacterProfile,
    availableVoices: List<VoiceDescriptor>,
    offlineVoices: List<VoiceDescriptor>,
    onProviderChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onTest: (CharacterProfile) -> Unit,
    onSave: (CharacterProfile) -> Unit,
    onBuildVoicePromptPreview: (CharacterProfile) -> String,
    onBuildSsmlPreview: (CharacterProfile, String) -> String
) {
    val isDefaultNarrator = initial.characterName == AppConstants.DEFAULT_NARRATOR_NAME
    var name by remember { mutableStateOf(initial.characterName) }
    var provider by remember { mutableStateOf(initial.voiceProvider) }
    var voiceId by remember { mutableStateOf(initial.voiceId) }
    var offlineVoiceId by remember { mutableStateOf(initial.offlineVoiceId) }
    var personality by remember { mutableStateOf(initial.voicePersonality) }
    var emotion by remember { mutableStateOf(initial.emotion) }
    var customDescription by remember { mutableStateOf(initial.customVoiceDescription) }
    var pitch by remember { mutableStateOf(initial.pitch) }
    var rate by remember { mutableStateOf(initial.speechRate) }
    var volume by remember { mutableStateOf(initial.volume) }
    var enabled by remember { mutableStateOf(initial.enabled) }
    var ssmlStyle by remember { mutableStateOf(initial.customSsmlStyle) }

    val draft = initial.copy(
        characterName = name,
        voiceProvider = provider,
        voiceId = voiceId,
        offlineVoiceId = offlineVoiceId,
        voicePersonality = personality,
        emotion = emotion,
        customVoiceDescription = customDescription,
        pitch = pitch,
        speechRate = rate,
        volume = volume,
        enabled = enabled,
        customSsmlStyle = ssmlStyle
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberColors.SurfaceNavy,
        title = {
            Text(
                text = if (initial.id == 0L) "Tambah Karakter" else "Edit Profil Suara Karakter",
                color = CyberColors.TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    enabled = !isDefaultNarrator,
                    label = { Text("Nama Karakter (mis. NORA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Provider Suara Online/Utama", fontSize = 12.sp, color = CyberColors.TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            provider = AppConstants.PROVIDER_GEMINI_TTS
                            voiceId = personality.defaultGeminiVoice
                            onProviderChanged(provider)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Gemini AI", fontSize = 10.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            provider = AppConstants.PROVIDER_EDGE_COMPATIBLE
                            onProviderChanged(provider)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Edge WS", fontSize = 10.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            provider = AppConstants.PROVIDER_ANDROID_TTS
                            onProviderChanged(provider)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Android", fontSize = 10.sp, maxLines = 1)
                    }
                }

                Text("Anime-Inspired Voice Personality", fontSize = 12.sp, color = CyberColors.TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VoicePersonality.entries.forEach { p ->
                        FilterChip(
                            selected = personality == p,
                            onClick = {
                                personality = p
                                if (provider == AppConstants.PROVIDER_GEMINI_TTS) {
                                    voiceId = p.defaultGeminiVoice
                                }
                            },
                            label = { Text(p.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Emotion Control", fontSize = 12.sp, color = CyberColors.TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VoiceEmotion.entries.forEach { em ->
                        FilterChip(
                            selected = emotion == em,
                            onClick = { emotion = em },
                            label = { Text(em.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = customDescription,
                    onValueChange = { customDescription = it },
                    label = { Text("Custom Voice Description (Voice Design)") },
                    placeholder = {
                        Text(
                            "Suara perempuan dewasa bergaya anime Jepang, lembut, ceria...",
                            fontSize = 11.sp
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = voiceId,
                    onValueChange = { voiceId = it },
                    label = { Text("Voice ID Online (mis. Kore / Zephyr / id-ID-GadisNeural)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (availableVoices.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableVoices.take(5).forEach { v ->
                            OutlinedButton(onClick = { voiceId = v.id }) {
                                Text(v.id, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = offlineVoiceId,
                    onValueChange = { offlineVoiceId = it },
                    label = { Text("Offline Fallback Voice ID (Android TTS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (offlineVoices.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        offlineVoices.take(4).forEach { ov ->
                            OutlinedButton(onClick = { offlineVoiceId = ov.id }) {
                                Text(ov.displayName, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }

                LabeledSlider(
                    label = "Pitch",
                    value = pitch,
                    valueRange = 0.5f..2.0f,
                    onValueChange = { pitch = it }
                )
                LabeledSlider(
                    label = "Speech Rate",
                    value = rate,
                    valueRange = 0.5f..2.0f,
                    onValueChange = { rate = it }
                )
                LabeledSlider(
                    label = "Volume",
                    value = volume,
                    valueRange = 0.0f..1.0f,
                    onValueChange = { volume = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aktifkan Dubbing", color = CyberColors.TextPrimary, fontSize = 13.sp)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                Text(
                    text = if (provider == AppConstants.PROVIDER_EDGE_COMPATIBLE) {
                        onBuildSsmlPreview(draft, "Halo, senang bertemu denganmu.")
                    } else {
                        "AI Voice Design Prompt:\n${onBuildVoicePromptPreview(draft)}"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = CyberColors.TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(draft) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberColors.CyberCyan,
                    contentColor = CyberColors.DarkNavy
                )
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onTest(draft) }) {
                    Text("Test Voice", color = CyberColors.EmeraldActive)
                }
                TextButton(onClick = onDismiss) {
                    Text("Batal", color = CyberColors.TextSecondary)
                }
            }
        }
    )
}

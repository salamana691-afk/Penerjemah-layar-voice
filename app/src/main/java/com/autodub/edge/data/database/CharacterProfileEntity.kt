package com.autodub.edge.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.domain.model.CharacterProfile
import com.autodub.edge.domain.model.VoiceEmotion
import com.autodub.edge.domain.model.VoicePersonality

@Entity(
    tableName = "character_profiles",
    indices = [
        Index(value = ["character_name"], unique = true)
    ]
)
data class CharacterProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "character_name")
    val characterName: String,

    @ColumnInfo(name = "voice_provider")
    val voiceProvider: String,

    @ColumnInfo(name = "voice_id")
    val voiceId: String,

    @ColumnInfo(name = "offline_voice_id", defaultValue = "")
    val offlineVoiceId: String = "",

    @ColumnInfo(name = "voice_personality", defaultValue = "ANIME_FEMALE_GENTLE")
    val voicePersonality: String = VoicePersonality.ANIME_FEMALE_GENTLE.id,

    @ColumnInfo(name = "emotion", defaultValue = "NEUTRAL")
    val emotion: String = VoiceEmotion.NEUTRAL.id,

    @ColumnInfo(name = "custom_voice_description", defaultValue = "")
    val customVoiceDescription: String = "",

    @ColumnInfo(name = "pitch")
    val pitch: Float,

    @ColumnInfo(name = "speech_rate")
    val speechRate: Float,

    @ColumnInfo(name = "volume")
    val volume: Float,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean,

    @ColumnInfo(name = "custom_ssml_style", defaultValue = "")
    val customSsmlStyle: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    fun toDomain(): CharacterProfile = CharacterProfile(
        id = id,
        characterName = characterName,
        voiceProvider = voiceProvider,
        voiceId = voiceId,
        offlineVoiceId = offlineVoiceId,
        voicePersonality = VoicePersonality.fromId(voicePersonality),
        emotion = VoiceEmotion.fromId(emotion),
        customVoiceDescription = customVoiceDescription,
        pitch = pitch,
        speechRate = speechRate,
        volume = volume,
        enabled = enabled,
        customSsmlStyle = customSsmlStyle,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(profile: CharacterProfile): CharacterProfileEntity = CharacterProfileEntity(
            id = profile.id,
            characterName = profile.characterName,
            voiceProvider = profile.voiceProvider,
            voiceId = profile.voiceId,
            offlineVoiceId = profile.offlineVoiceId,
            voicePersonality = profile.voicePersonality.id,
            emotion = profile.emotion.id,
            customVoiceDescription = profile.customVoiceDescription,
            pitch = profile.pitch,
            speechRate = profile.speechRate,
            volume = profile.volume,
            enabled = profile.enabled,
            customSsmlStyle = profile.customSsmlStyle,
            createdAt = profile.createdAt,
            updatedAt = profile.updatedAt
        )

        fun defaultNarrator(now: Long = System.currentTimeMillis()): CharacterProfileEntity =
            CharacterProfileEntity(
                characterName = AppConstants.DEFAULT_NARRATOR_NAME,
                voiceProvider = AppConstants.PROVIDER_GEMINI_TTS,
                voiceId = "Charon",
                offlineVoiceId = "",
                voicePersonality = VoicePersonality.ANIME_MALE_CALM.id,
                emotion = VoiceEmotion.NEUTRAL.id,
                customVoiceDescription = "Suara narator dewasa yang tenang, jernih, netral, dan mudah dipahami.",
                pitch = 1.0f,
                speechRate = 1.0f,
                volume = 1.0f,
                enabled = true,
                customSsmlStyle = "",
                createdAt = now,
                updatedAt = now
            )
    }
}

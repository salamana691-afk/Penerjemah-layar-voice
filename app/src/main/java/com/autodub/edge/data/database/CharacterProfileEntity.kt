package com.autodub.edge.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.domain.model.CharacterProfile

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
                voiceProvider = AppConstants.PROVIDER_ANDROID_TTS,
                voiceId = "",
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

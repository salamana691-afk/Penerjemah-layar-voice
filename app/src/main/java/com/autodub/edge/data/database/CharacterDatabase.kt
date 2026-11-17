package com.autodub.edge.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.domain.model.VoiceEmotion
import com.autodub.edge.domain.model.VoicePersonality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CharacterProfileEntity::class],
    version = 3,
    exportSchema = true
)
abstract class CharacterDatabase : RoomDatabase() {

    abstract fun characterDao(): CharacterDao

    companion object {
        private const val DB_NAME = "autodub_edge.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE character_profiles ADD COLUMN custom_ssml_style TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE character_profiles ADD COLUMN offline_voice_id TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE character_profiles ADD COLUMN voice_personality TEXT NOT NULL DEFAULT 'ANIME_FEMALE_GENTLE'"
                )
                db.execSQL(
                    "ALTER TABLE character_profiles ADD COLUMN emotion TEXT NOT NULL DEFAULT 'NEUTRAL'"
                )
                db.execSQL(
                    "ALTER TABLE character_profiles ADD COLUMN custom_voice_description TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun build(context: Context): CharacterDatabase {
            lateinit var instance: CharacterDatabase
            instance = Room.databaseBuilder(
                context.applicationContext,
                CharacterDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedDefaults(instance.characterDao())
                        }
                    }
                })
                .build()
            return instance
        }

        suspend fun seedDefaults(dao: CharacterDao) {
            val now = System.currentTimeMillis()
            val defaults = listOf(
                CharacterProfileEntity.defaultNarrator(now),
                CharacterProfileEntity(
                    characterName = "NORA",
                    voiceProvider = AppConstants.PROVIDER_GEMINI_TTS,
                    voiceId = "Zephyr",
                    offlineVoiceId = "",
                    voicePersonality = VoicePersonality.ANIME_FEMALE_CHEERFUL.id,
                    emotion = VoiceEmotion.HAPPY.id,
                    customVoiceDescription = "Suara perempuan dewasa bergaya anime Jepang, lembut, ceria, sedikit manja, dengan intonasi ekspresif dan artikulasi jelas.",
                    pitch = 1.08f,
                    speechRate = 1.04f,
                    volume = 1.0f,
                    enabled = true,
                    customSsmlStyle = "cheerful",
                    createdAt = now,
                    updatedAt = now
                ),
                CharacterProfileEntity(
                    characterName = "SALMAN",
                    voiceProvider = AppConstants.PROVIDER_GEMINI_TTS,
                    voiceId = "Charon",
                    offlineVoiceId = "",
                    voicePersonality = VoicePersonality.ANIME_MALE_CALM.id,
                    emotion = VoiceEmotion.CALM.id,
                    customVoiceDescription = "Suara laki-laki dewasa bergaya anime Jepang yang kalem, berwibawa, dan tenang.",
                    pitch = 0.95f,
                    speechRate = 0.96f,
                    volume = 1.0f,
                    enabled = true,
                    customSsmlStyle = "calm",
                    createdAt = now,
                    updatedAt = now
                )
            )
            dao.insertAllIgnore(defaults)
        }
    }
}

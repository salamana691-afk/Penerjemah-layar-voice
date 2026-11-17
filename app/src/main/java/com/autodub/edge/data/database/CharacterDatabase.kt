package com.autodub.edge.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.autodub.edge.core.constants.AppConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CharacterProfileEntity::class],
    version = 2,
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

        fun build(context: Context): CharacterDatabase {
            lateinit var instance: CharacterDatabase
            instance = Room.databaseBuilder(
                context.applicationContext,
                CharacterDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2)
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
                    voiceProvider = AppConstants.PROVIDER_ANDROID_TTS,
                    voiceId = "",
                    pitch = 1.10f,
                    speechRate = 1.05f,
                    volume = 1.0f,
                    enabled = true,
                    customSsmlStyle = "cheerful",
                    createdAt = now,
                    updatedAt = now
                ),
                CharacterProfileEntity(
                    characterName = "SALMAN",
                    voiceProvider = AppConstants.PROVIDER_ANDROID_TTS,
                    voiceId = "",
                    pitch = 0.95f,
                    speechRate = 0.95f,
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

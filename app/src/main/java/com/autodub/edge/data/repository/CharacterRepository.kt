package com.autodub.edge.data.repository

import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.util.AppError
import com.autodub.edge.core.util.AppResult
import com.autodub.edge.core.util.TextNormalizer
import com.autodub.edge.data.database.CharacterDao
import com.autodub.edge.data.database.CharacterDatabase
import com.autodub.edge.data.database.CharacterProfileEntity
import com.autodub.edge.domain.model.CharacterProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CharacterRepository(
    private val characterDao: CharacterDao
) {

    fun observeCharacters(): Flow<List<CharacterProfile>> =
        characterDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    suspend fun ensureSeeded() {
        withContext(Dispatchers.IO) {
            runCatching {
                val existing = characterDao.getAllOnce()
                if (existing.isEmpty()) {
                    CharacterDatabase.seedDefaults(characterDao)
                } else if (existing.none { it.characterName.equals(AppConstants.DEFAULT_NARRATOR_NAME, ignoreCase = true) }) {
                    characterDao.insert(CharacterProfileEntity.defaultNarrator())
                }
            }
        }
    }

    suspend fun getAllCharacters(): AppResult<List<CharacterProfile>> = withContext(Dispatchers.IO) {
        try {
            AppResult.Success(characterDao.getAllOnce().map { it.toDomain() })
        } catch (t: Throwable) {
            AppResult.Error(AppError.DatabaseFailed(t.message ?: "Gagal membaca karakter", t))
        }
    }

    suspend fun getById(id: Long): AppResult<CharacterProfile?> = withContext(Dispatchers.IO) {
        try {
            AppResult.Success(characterDao.getById(id)?.toDomain())
        } catch (t: Throwable) {
            AppResult.Error(AppError.DatabaseFailed(t.message ?: "Gagal mengambil profil", t))
        }
    }

    suspend fun saveCharacter(profile: CharacterProfile): AppResult<Long> = withContext(Dispatchers.IO) {
        try {
            val normalizedName = if (profile.characterName.equals(AppConstants.DEFAULT_NARRATOR_NAME, ignoreCase = true)) {
                AppConstants.DEFAULT_NARRATOR_NAME
            } else {
                TextNormalizer.normalizeCharacterName(profile.characterName)
            }
            if (normalizedName.isBlank()) {
                return@withContext AppResult.Error(AppError.DatabaseFailed("Nama karakter tidak boleh kosong."))
            }

            val now = System.currentTimeMillis()
            val sanitized = profile.copy(
                characterName = normalizedName,
                pitch = profile.pitch.coerceIn(0.5f, 2.0f),
                speechRate = profile.speechRate.coerceIn(0.5f, 2.0f),
                volume = profile.volume.coerceIn(0.0f, 1.0f),
                updatedAt = now,
                createdAt = if (profile.id == 0L) now else profile.createdAt
            )

            val existingSameName = characterDao.getByName(normalizedName)
            val targetId = if (sanitized.id == 0L && existingSameName != null) {
                existingSameName.id
            } else {
                sanitized.id
            }

            val entity = CharacterProfileEntity.fromDomain(sanitized.copy(id = targetId))
            val savedId = if (entity.id == 0L) {
                characterDao.insert(entity)
            } else {
                characterDao.update(entity)
                entity.id
            }
            AppResult.Success(savedId)
        } catch (t: Throwable) {
            AppResult.Error(AppError.DatabaseFailed(t.message ?: "Gagal menyimpan karakter", t))
        }
    }

    suspend fun deleteCharacter(id: Long): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val target = characterDao.getById(id)
            if (target != null && target.characterName == AppConstants.DEFAULT_NARRATOR_NAME) {
                return@withContext AppResult.Error(
                    AppError.DatabaseFailed("Default Narrator tidak dapat dihapus.")
                )
            }
            characterDao.deleteById(id)
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            AppResult.Error(AppError.DatabaseFailed(t.message ?: "Gagal menghapus karakter", t))
        }
    }
}

package com.autodub.edge.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {

    @Query("SELECT * FROM character_profiles ORDER BY CASE WHEN character_name = 'DEFAULT_NARRATOR' THEN 0 ELSE 1 END, character_name ASC")
    fun observeAll(): Flow<List<CharacterProfileEntity>>

    @Query("SELECT * FROM character_profiles ORDER BY character_name ASC")
    suspend fun getAllOnce(): List<CharacterProfileEntity>

    @Query("SELECT * FROM character_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CharacterProfileEntity?

    @Query("SELECT * FROM character_profiles WHERE UPPER(character_name) = UPPER(:name) LIMIT 1")
    suspend fun getByName(name: String): CharacterProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CharacterProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entities: List<CharacterProfileEntity>)

    @Update
    suspend fun update(entity: CharacterProfileEntity)

    @Delete
    suspend fun delete(entity: CharacterProfileEntity)

    @Query("DELETE FROM character_profiles WHERE id = :id AND character_name != 'DEFAULT_NARRATOR'")
    suspend fun deleteById(id: Long)
}

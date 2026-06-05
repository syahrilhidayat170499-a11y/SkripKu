package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters WHERE projectId = :projectId ORDER BY name ASC")
    fun getCharactersForProjectFlow(projectId: Int): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE projectId = :projectId ORDER BY name ASC")
    suspend fun getCharactersForProject(projectId: Int): List<CharacterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity): Long

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)
}

package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.SceneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SceneDao {
    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY sequenceNumber ASC")
    fun getScenesForProjectFlow(projectId: Int): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY sequenceNumber ASC")
    suspend fun getScenesForProject(projectId: Int): List<SceneEntity>

    @Query("SELECT * FROM scenes WHERE id = :id LIMIT 1")
    suspend fun getSceneById(id: Int): SceneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: SceneEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<SceneEntity>)

    @Update
    suspend fun updateScene(scene: SceneEntity)

    @Delete
    suspend fun deleteScene(scene: SceneEntity)

    @Query("DELETE FROM scenes WHERE projectId = :projectId")
    suspend fun deleteScenesForProject(projectId: Int)
}

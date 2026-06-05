package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations WHERE projectId = :projectId ORDER BY name ASC")
    fun getLocationsForProjectFlow(projectId: Int): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE projectId = :projectId ORDER BY name ASC")
    suspend fun getLocationsForProject(projectId: Int): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)
}

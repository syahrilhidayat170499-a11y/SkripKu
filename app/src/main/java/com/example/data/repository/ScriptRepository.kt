package com.example.data.repository

import com.example.data.local.dao.CharacterDao
import com.example.data.local.dao.LocationDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.SceneDao
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SceneEntity
import kotlinx.coroutines.flow.Flow

class ScriptRepository(
    private val projectDao: ProjectDao,
    private val sceneDao: SceneDao,
    private val characterDao: CharacterDao,
    private val locationDao: LocationDao
) {
    // --- Projects ---
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>> = projectDao.getAllProjectsFlow()
    
    suspend fun getProjectById(id: Int): ProjectEntity? = projectDao.getProjectById(id)
    
    suspend fun insertProject(project: ProjectEntity): Long = projectDao.insertProject(project)
    
    suspend fun updateProject(project: ProjectEntity) = projectDao.updateProject(project)
    
    suspend fun deleteProject(project: ProjectEntity) = projectDao.deleteProject(project)

    // --- Scenes ---
    fun getScenesForProjectFlow(projectId: Int): Flow<List<SceneEntity>> = sceneDao.getScenesForProjectFlow(projectId)
    
    suspend fun getScenesForProject(projectId: Int): List<SceneEntity> = sceneDao.getScenesForProject(projectId)
    
    suspend fun getSceneById(id: Int): SceneEntity? = sceneDao.getSceneById(id)
    
    suspend fun insertScene(scene: SceneEntity): Long = sceneDao.insertScene(scene)
    
    suspend fun insertScenes(scenes: List<SceneEntity>) = sceneDao.insertScenes(scenes)
    
    suspend fun updateScene(scene: SceneEntity) = sceneDao.updateScene(scene)
    
    suspend fun deleteScene(scene: SceneEntity) = sceneDao.deleteScene(scene)
    
    suspend fun deleteScenesForProject(projectId: Int) = sceneDao.deleteScenesForProject(projectId)

    // --- Characters ---
    fun getCharactersForProjectFlow(projectId: Int): Flow<List<CharacterEntity>> = characterDao.getCharactersForProjectFlow(projectId)
    
    suspend fun getCharactersForProject(projectId: Int): List<CharacterEntity> = characterDao.getCharactersForProject(projectId)
    
    suspend fun insertCharacter(character: CharacterEntity): Long = characterDao.insertCharacter(character)
    
    suspend fun updateCharacter(character: CharacterEntity) = characterDao.updateCharacter(character)
    
    suspend fun deleteCharacter(character: CharacterEntity) = characterDao.deleteCharacter(character)

    // --- Locations ---
    fun getLocationsForProjectFlow(projectId: Int): Flow<List<LocationEntity>> = locationDao.getLocationsForProjectFlow(projectId)
    
    suspend fun getLocationsForProject(projectId: Int): List<LocationEntity> = locationDao.getLocationsForProject(projectId)
    
    suspend fun insertLocation(location: LocationEntity): Long = locationDao.insertLocation(location)
    
    suspend fun updateLocation(location: LocationEntity) = locationDao.updateLocation(location)
    
    suspend fun deleteLocation(location: LocationEntity) = locationDao.deleteLocation(location)
}

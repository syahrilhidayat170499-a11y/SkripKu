package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.CharacterDao
import com.example.data.local.dao.LocationDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.SceneDao
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SceneEntity

@Database(
    entities = [
        ProjectEntity::class,
        SceneEntity::class,
        CharacterEntity::class,
        LocationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun sceneDao(): SceneDao
    abstract fun characterDao(): CharacterDao
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scripku_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

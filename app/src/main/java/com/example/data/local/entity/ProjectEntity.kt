package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val logline: String,
    val genre: String,
    val targetDuration: Int, // in minutes
    val structureType: String, // "3_act", "heros_journey", "save_the_cat", "blank"
    val createdAt: Long = System.currentTimeMillis()
)

package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scenes")
data class SceneEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val title: String,
    val sequenceNumber: Int,
    val summary: String,
    val contentJson: String // Serialized JSON containing list of script elements
)

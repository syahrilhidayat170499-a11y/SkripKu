package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val name: String,
    val type: String, // "INT", "EXT", "INT/EXT"
    val timeOfDay: String, // "SIANG", "MALAM", "PAGI", "SORE"
    val description: String
)

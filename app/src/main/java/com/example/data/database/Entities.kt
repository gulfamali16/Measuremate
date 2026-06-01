package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val type: String, // "LENGTH", "HEIGHT", "AREA"
    val valueMeters: Double,
    val unit: String, // "M", "CM", "FT", "IN"
    val pointsJson: String = "[]",
    val screenshotPath: String? = null,
    val note: String = "",
    val confidence: Int = 92, // Percentage
    val createdAt: Long = System.currentTimeMillis()
)

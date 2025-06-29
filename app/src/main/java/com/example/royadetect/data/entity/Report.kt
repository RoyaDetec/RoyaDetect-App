package com.example.royadetect.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val severityLevel: Int,
    val confidence: Float,
    val pdfPath: String,
    val timestamp: Long = System.currentTimeMillis()
)
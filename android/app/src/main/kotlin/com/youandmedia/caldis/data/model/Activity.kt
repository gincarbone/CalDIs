package com.youandmedia.caldis.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val caloriesBurned: Double,
    val category: String,
    val description: String,
    val date: Long // epoch millis
)

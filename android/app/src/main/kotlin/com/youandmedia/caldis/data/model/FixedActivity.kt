package com.youandmedia.caldis.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fixed_activities")
data class FixedActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val caloriesBurned: Double
)

package com.youandmedia.caldis.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fixed_meals")
data class FixedMeal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val calories: Double,
    val description: String = ""
)

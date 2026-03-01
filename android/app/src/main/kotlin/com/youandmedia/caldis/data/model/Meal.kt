package com.youandmedia.caldis.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val calories: Double,
    val category: String,
    val description: String,
    val date: Long, // epoch millis
    val photoUri: String? = null
)

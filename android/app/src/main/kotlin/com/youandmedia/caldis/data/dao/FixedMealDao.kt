package com.youandmedia.caldis.data.dao

import androidx.room.*
import com.youandmedia.caldis.data.model.FixedMeal
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedMealDao {
    @Insert
    suspend fun insert(fixedMeal: FixedMeal)

    @Update
    suspend fun update(fixedMeal: FixedMeal)

    @Delete
    suspend fun delete(fixedMeal: FixedMeal)

    @Query("SELECT * FROM fixed_meals")
    fun getAll(): Flow<List<FixedMeal>>

    @Query("SELECT * FROM fixed_meals")
    suspend fun getAllOnce(): List<FixedMeal>
}

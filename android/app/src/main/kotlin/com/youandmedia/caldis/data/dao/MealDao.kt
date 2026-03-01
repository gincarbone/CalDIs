package com.youandmedia.caldis.data.dao

import androidx.room.*
import com.youandmedia.caldis.data.model.Meal
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert
    suspend fun insert(meal: Meal)

    @Update
    suspend fun update(meal: Meal)

    @Delete
    suspend fun delete(meal: Meal)

    @Query("SELECT * FROM meals WHERE date >= :startMillis AND date <= :endMillis")
    fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<Meal>>

    @Query("SELECT * FROM meals WHERE date >= :startMillis AND date <= :endMillis")
    suspend fun getByDateRangeOnce(startMillis: Long, endMillis: Long): List<Meal>

    @Query("SELECT * FROM meals")
    fun getAll(): Flow<List<Meal>>

    @Query("SELECT * FROM meals")
    suspend fun getAllOnce(): List<Meal>
}

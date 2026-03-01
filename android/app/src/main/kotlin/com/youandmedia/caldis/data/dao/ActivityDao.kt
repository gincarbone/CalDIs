package com.youandmedia.caldis.data.dao

import androidx.room.*
import com.youandmedia.caldis.data.model.Activity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(activity: Activity)

    @Update
    suspend fun update(activity: Activity)

    @Delete
    suspend fun delete(activity: Activity)

    @Query("SELECT * FROM activities WHERE date >= :startMillis AND date <= :endMillis")
    fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE date >= :startMillis AND date <= :endMillis")
    suspend fun getByDateRangeOnce(startMillis: Long, endMillis: Long): List<Activity>

    @Query("SELECT * FROM activities")
    fun getAll(): Flow<List<Activity>>

    @Query("SELECT * FROM activities")
    suspend fun getAllOnce(): List<Activity>
}

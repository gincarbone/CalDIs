package com.youandmedia.caldis.data.dao

import androidx.room.*
import com.youandmedia.caldis.data.model.FixedActivity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedActivityDao {
    @Insert
    suspend fun insert(fixedActivity: FixedActivity)

    @Update
    suspend fun update(fixedActivity: FixedActivity)

    @Delete
    suspend fun delete(fixedActivity: FixedActivity)

    @Query("SELECT * FROM fixed_activities")
    fun getAll(): Flow<List<FixedActivity>>

    @Query("SELECT * FROM fixed_activities")
    suspend fun getAllOnce(): List<FixedActivity>
}

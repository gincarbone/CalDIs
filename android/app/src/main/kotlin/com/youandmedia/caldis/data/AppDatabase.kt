package com.youandmedia.caldis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.youandmedia.caldis.data.dao.*
import com.youandmedia.caldis.data.model.*

@Database(
    entities = [Meal::class, Activity::class, FixedMeal::class, FixedActivity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun activityDao(): ActivityDao
    abstract fun fixedMealDao(): FixedMealDao
    abstract fun fixedActivityDao(): FixedActivityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "caldis_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

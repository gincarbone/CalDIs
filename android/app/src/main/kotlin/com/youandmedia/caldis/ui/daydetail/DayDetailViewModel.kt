package com.youandmedia.caldis.ui.daydetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youandmedia.caldis.CaldisApp
import com.youandmedia.caldis.data.model.Activity
import com.youandmedia.caldis.data.model.Meal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class DayDetailUiState(
    val meals: List<Meal> = emptyList(),
    val activities: List<Activity> = emptyList()
)

class DayDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as CaldisApp).database

    private val _uiState = MutableStateFlow(DayDetailUiState())
    val uiState: StateFlow<DayDetailUiState> = _uiState

    fun loadDay(date: LocalDate) {
        viewModelScope.launch {
            val startMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val meals = db.mealDao().getByDateRangeOnce(startMillis, endMillis)
            val activities = db.activityDao().getByDateRangeOnce(startMillis, endMillis)

            _uiState.value = DayDetailUiState(meals = meals, activities = activities)
        }
    }

    fun saveMeal(calories: Double, category: String, description: String, date: LocalDate) {
        viewModelScope.launch {
            val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            db.mealDao().insert(Meal(calories = calories, category = category, description = description, date = millis))
            loadDay(date)
        }
    }

    fun updateMeal(meal: Meal, calories: Double, category: String, description: String, date: LocalDate) {
        viewModelScope.launch {
            db.mealDao().update(meal.copy(calories = calories, category = category, description = description))
            loadDay(date)
        }
    }

    fun deleteMeal(meal: Meal, date: LocalDate) {
        viewModelScope.launch {
            db.mealDao().delete(meal)
            loadDay(date)
        }
    }

    fun saveActivity(caloriesBurned: Double, category: String, description: String, date: LocalDate) {
        viewModelScope.launch {
            val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            db.activityDao().insert(Activity(caloriesBurned = caloriesBurned, category = category, description = description, date = millis))
            loadDay(date)
        }
    }

    fun updateActivity(activity: Activity, caloriesBurned: Double, category: String, description: String, date: LocalDate) {
        viewModelScope.launch {
            db.activityDao().update(activity.copy(caloriesBurned = caloriesBurned, category = category, description = description))
            loadDay(date)
        }
    }

    fun deleteActivity(activity: Activity, date: LocalDate) {
        viewModelScope.launch {
            db.activityDao().delete(activity)
            loadDay(date)
        }
    }
}

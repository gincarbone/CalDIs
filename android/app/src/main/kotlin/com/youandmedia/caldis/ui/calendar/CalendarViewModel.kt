package com.youandmedia.caldis.ui.calendar

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youandmedia.caldis.CaldisApp
import com.youandmedia.caldis.data.model.Meal
import com.youandmedia.caldis.util.DEFAULT_DAILY_CALORIE_BUDGET
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class CalendarUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val datesWithMeals: Set<LocalDate> = emptySet(),
    val datesWithActivities: Set<LocalDate> = emptySet(),
    val dailyCalorieBudget: Double = DEFAULT_DAILY_CALORIE_BUDGET,
    val totalCaloriesOfMonth: Double = 0.0,
    val totalBurnedOfMonth: Double = 0.0,
    val totalFixedMealCalories: Double = 0.0,
    val totalFixedActivityCalories: Double = 0.0,
    val forecastCaloriesOfMonth: Double = 0.0,
    val dailyCalories: Map<LocalDate, Double> = emptyMap()
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as CaldisApp).database
    private val prefs = application.getSharedPreferences("caldis_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState

    init {
        loadData()
    }

    fun onMonthChanged(yearMonth: YearMonth) {
        _uiState.value = _uiState.value.copy(selectedMonth = yearMonth)
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val month = _uiState.value.selectedMonth
            val startOfMonth = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfMonth = month.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val meals = db.mealDao().getByDateRangeOnce(startOfMonth, endOfMonth)
            val activities = db.activityDao().getByDateRangeOnce(startOfMonth, endOfMonth)
            val fixedMeals = db.fixedMealDao().getAllOnce()
            val fixedActivities = db.fixedActivityDao().getAllOnce()

            val datesWithM = meals.map { epochToLocalDate(it.date) }.toSet()
            val datesWithA = activities.map { epochToLocalDate(it.date) }.toSet()

            val totalCalMonth = meals.sumOf { it.calories }
            val totalBurnedMonth = activities.sumOf { it.caloriesBurned }
            val totalFixedMealCal = fixedMeals.sumOf { it.calories }
            val totalFixedActCal = fixedActivities.sumOf { it.caloriesBurned }

            val forecast = calculateForecast(meals, month)

            val savedBudget = prefs.getFloat("daily_calorie_budget", DEFAULT_DAILY_CALORIE_BUDGET.toFloat()).toDouble()

            val dailyCal = meals.groupBy { epochToLocalDate(it.date) }
                .mapValues { (_, list) -> list.sumOf { it.calories } }

            _uiState.value = _uiState.value.copy(
                datesWithMeals = datesWithM,
                datesWithActivities = datesWithA,
                dailyCalorieBudget = savedBudget,
                totalCaloriesOfMonth = totalCalMonth,
                totalBurnedOfMonth = totalBurnedMonth,
                totalFixedMealCalories = totalFixedMealCal,
                totalFixedActivityCalories = totalFixedActCal,
                forecastCaloriesOfMonth = forecast,
                dailyCalories = dailyCal
            )
        }
    }

    fun quickAddMeal(calories: Double, category: String, description: String, date: LocalDate) {
        viewModelScope.launch {
            val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            db.mealDao().insert(
                Meal(calories = calories, category = category, description = description, date = millis)
            )
            loadData()
        }
    }

    fun quickAddActivity(caloriesBurned: Double, category: String, description: String, date: LocalDate) {
        viewModelScope.launch {
            val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            db.activityDao().insert(
                com.youandmedia.caldis.data.model.Activity(
                    caloriesBurned = caloriesBurned, category = category, description = description, date = millis
                )
            )
            loadData()
        }
    }

    private fun calculateForecast(meals: List<Meal>, month: YearMonth): Double {
        val now = LocalDate.now()
        if (month != YearMonth.now() || now.dayOfMonth == 0) return 0.0

        val todayEnd = now.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val startOfMonth = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val mealsUntilToday = meals.filter { it.date <= todayEnd && it.date >= startOfMonth }
        val totalUntilToday = mealsUntilToday.sumOf { it.calories }

        val daysPassed = now.dayOfMonth
        val totalDays = month.lengthOfMonth()

        return if (daysPassed > 0) totalUntilToday / daysPassed * totalDays else 0.0
    }

    private fun epochToLocalDate(epochMillis: Long): LocalDate {
        return java.time.Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    fun isCurrentMonth(): Boolean {
        return _uiState.value.selectedMonth == YearMonth.now()
    }
}

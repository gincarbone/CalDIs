package com.youandmedia.caldis.ui.stats

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youandmedia.caldis.CaldisApp
import com.youandmedia.caldis.util.DEFAULT_DAILY_CALORIE_BUDGET
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class CategoryStat(
    val category: String,
    val calories: Double,
    val percentage: Float,
    val entryCount: Int
)

data class MonthTrend(
    val month: YearMonth,
    val totalCalories: Double,
    val totalBurned: Double
)

data class PeriodTrend(
    val label: String,
    val totalCalories: Double,
    val totalBurned: Double
)

data class StatsUiState(
    val categoryStats: List<CategoryStat> = emptyList(),
    val totalCalories: Double = 0.0,
    val totalBurned: Double = 0.0,
    val selectedMonth: YearMonth = YearMonth.now(),
    val topCategory: String = "",
    val avgDailyCalories: Double = 0.0,
    val entryCount: Int = 0,
    val monthTrends: List<MonthTrend> = emptyList(),
    val weeklyTrends: List<PeriodTrend> = emptyList(),
    val dailyTrendsLast7: List<PeriodTrend> = emptyList(),
    val dailyBudget: Double = DEFAULT_DAILY_CALORIE_BUDGET
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as CaldisApp).database

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState

    init {
        loadMonthData(YearMonth.now())
    }

    fun loadMonthData(month: YearMonth) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val startMillis = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = month.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

            val meals = db.mealDao().getByDateRangeOnce(startMillis, endMillis)
            val activities = db.activityDao().getByDateRangeOnce(startMillis, endMillis)
            val prefs = getApplication<CaldisApp>().getSharedPreferences("caldis_prefs", Context.MODE_PRIVATE)
            val dailyBudget = prefs.getFloat("daily_calorie_budget", DEFAULT_DAILY_CALORIE_BUDGET.toFloat()).toDouble()

            val categoryMap = mutableMapOf<String, Pair<Double, Int>>()
            var total = 0.0

            for (meal in meals) {
                total += meal.calories
                val current = categoryMap[meal.category] ?: Pair(0.0, 0)
                categoryMap[meal.category] = Pair(current.first + meal.calories, current.second + 1)
            }

            val stats = categoryMap.entries
                .map { (cat, pair) ->
                    CategoryStat(
                        category = cat,
                        calories = pair.first,
                        percentage = if (total > 0) (pair.first / total * 100).toFloat() else 0f,
                        entryCount = pair.second
                    )
                }
                .sortedByDescending { it.calories }

            val daysInMonth = month.lengthOfMonth()
            val daysPassed = if (month == YearMonth.now()) LocalDate.now().dayOfMonth else daysInMonth

            val weeklyTrends = buildWeeklyTrends(month = month, meals = meals, activities = activities, zone = zone)

            val today = LocalDate.now()
            val last7Start = today.minusDays(6)
            val last7StartMillis = last7Start.atStartOfDay(zone).toInstant().toEpochMilli()
            val last7EndMillis = today.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
            val mealsLast7 = db.mealDao().getByDateRangeOnce(last7StartMillis, last7EndMillis)
            val activitiesLast7 = db.activityDao().getByDateRangeOnce(last7StartMillis, last7EndMillis)
            val dailyTrendsLast7 = buildDailyLast7Trends(
                startDate = last7Start,
                endDate = today,
                meals = mealsLast7,
                activities = activitiesLast7,
                zone = zone
            )

            _uiState.value = StatsUiState(
                categoryStats = stats,
                totalCalories = total,
                totalBurned = activities.sumOf { it.caloriesBurned },
                selectedMonth = month,
                topCategory = stats.firstOrNull()?.category ?: "-",
                avgDailyCalories = if (daysPassed > 0) total / daysPassed else 0.0,
                entryCount = meals.size,
                weeklyTrends = weeklyTrends,
                dailyTrendsLast7 = dailyTrendsLast7,
                dailyBudget = dailyBudget
            )
        }
    }

    fun nextMonth() {
        loadMonthData(_uiState.value.selectedMonth.plusMonths(1))
    }

    fun prevMonth() {
        loadMonthData(_uiState.value.selectedMonth.minusMonths(1))
    }

    private fun buildWeeklyTrends(
        month: YearMonth,
        meals: List<com.youandmedia.caldis.data.model.Meal>,
        activities: List<com.youandmedia.caldis.data.model.Activity>,
        zone: ZoneId
    ): List<PeriodTrend> {
        val mealByDate = meals.groupBy { java.time.Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
        val activityByDate = activities.groupBy { java.time.Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
        val trends = mutableListOf<PeriodTrend>()

        var startDay = 1
        while (startDay <= month.lengthOfMonth()) {
            val endDay = minOf(startDay + 6, month.lengthOfMonth())
            var cal = 0.0
            var burned = 0.0

            for (d in startDay..endDay) {
                val date = month.atDay(d)
                cal += mealByDate[date]?.sumOf { it.calories } ?: 0.0
                burned += activityByDate[date]?.sumOf { it.caloriesBurned } ?: 0.0
            }

            trends.add(
                PeriodTrend(
                    label = "$startDay-$endDay",
                    totalCalories = cal,
                    totalBurned = burned
                )
            )
            startDay += 7
        }

        return trends
    }

    private fun buildDailyLast7Trends(
        startDate: LocalDate,
        endDate: LocalDate,
        meals: List<com.youandmedia.caldis.data.model.Meal>,
        activities: List<com.youandmedia.caldis.data.model.Activity>,
        zone: ZoneId
    ): List<PeriodTrend> {
        val mealByDate = meals.groupBy { java.time.Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
        val activityByDate = activities.groupBy { java.time.Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
        val trends = mutableListOf<PeriodTrend>()

        var date = startDate
        while (!date.isAfter(endDate)) {
            trends.add(
                PeriodTrend(
                    label = date.dayOfMonth.toString(),
                    totalCalories = mealByDate[date]?.sumOf { it.calories } ?: 0.0,
                    totalBurned = activityByDate[date]?.sumOf { it.caloriesBurned } ?: 0.0
                )
            )
            date = date.plusDays(1)
        }

        return trends
    }
}

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
import java.time.format.DateTimeFormatter

enum class MotivationBadgeType {
    WEEK,
    MONTH
}

data class MotivationBadge(
    val type: MotivationBadgeType,
    val title: String,
    val subtitle: String,
    val day: LocalDate,
    val savedCalories: Double
)

data class MotivationChallenge(
    val title: String,
    val subtitle: String,
    val progress: Float,
    val progressLabel: String,
    val isCompleted: Boolean
)

data class DailyMission(
    val title: String,
    val subtitle: String,
    val isCompleted: Boolean
)

data class MotivationProfile(
    val xp: Int,
    val level: Int,
    val nextLevelXp: Int
)

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
    val dailyBudget: Double = DEFAULT_DAILY_CALORIE_BUDGET,
    val motivationBadges: List<MotivationBadge> = emptyList(),
    val motivationChallenges: List<MotivationChallenge> = emptyList(),
    val dailyMission: DailyMission? = null,
    val motivationProfile: MotivationProfile = MotivationProfile(xp = 0, level = 1, nextLevelXp = 250)
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
            val motivationBadges = buildMotivationBadges(
                month = month,
                meals = meals,
                dailyBudget = dailyBudget,
                zone = zone
            )

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
            val burnedLast7 = activitiesLast7.sumOf { it.caloriesBurned }
            val challenges = buildMotivationChallenges(
                month = month,
                meals = meals,
                dailyBudget = dailyBudget,
                burnedLast7 = burnedLast7,
                zone = zone
            )
            val mission = buildDailyMission(
                mealsLast7 = mealsLast7,
                dailyBudget = dailyBudget,
                zone = zone
            )
            val profile = buildMotivationProfile(
                mealCount = meals.size,
                activityCount = activities.size,
                badgesCount = motivationBadges.size,
                challenges = challenges
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
                dailyBudget = dailyBudget,
                motivationBadges = motivationBadges,
                motivationChallenges = challenges,
                dailyMission = mission,
                motivationProfile = profile
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

    private fun buildMotivationBadges(
        month: YearMonth,
        meals: List<com.youandmedia.caldis.data.model.Meal>,
        dailyBudget: Double,
        zone: ZoneId
    ): List<MotivationBadge> {
        if (dailyBudget <= 0.0) return emptyList()

        val formatter = DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ITALIAN)
        val mealByDate = meals.groupBy { java.time.Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
            .mapValues { (_, dayMeals) -> dayMeals.sumOf { it.calories } }

        val today = LocalDate.now(zone)
        val monthEndForCurrent = today.minusDays(1)
        val effectiveEnd = if (month == YearMonth.now()) {
            if (monthEndForCurrent.month == month.month && monthEndForCurrent.year == month.year) monthEndForCurrent else month.atDay(1).minusDays(1)
        } else {
            month.atEndOfMonth()
        }

        if (effectiveEnd.isBefore(month.atDay(1))) return emptyList()

        val badges = mutableListOf<MotivationBadge>()

        val monthBest = findBestDayInRange(
            start = month.atDay(1),
            end = effectiveEnd,
            mealByDate = mealByDate,
            dailyBudget = dailyBudget
        )
        if (monthBest != null) {
            badges.add(
                MotivationBadge(
                    type = MotivationBadgeType.MONTH,
                    title = "Miglior giorno del mese",
                    subtitle = "${monthBest.first.format(formatter)} • ${monthBest.second.toInt()} kcal risparmiate",
                    day = monthBest.first,
                    savedCalories = monthBest.second
                )
            )
        }

        val weekIndex = if (month == YearMonth.now()) {
            ((today.dayOfMonth - 1) / 7) - 1
        } else {
            (month.lengthOfMonth() - 1) / 7
        }

        if (weekIndex >= 0) {
            val weekStart = month.atDay(weekIndex * 7 + 1)
            val weekEnd = minOf(weekStart.plusDays(6), effectiveEnd)
            val weekBest = findBestDayInRange(
                start = weekStart,
                end = weekEnd,
                mealByDate = mealByDate,
                dailyBudget = dailyBudget
            )
            if (weekBest != null) {
                badges.add(
                    MotivationBadge(
                        type = MotivationBadgeType.WEEK,
                        title = "Miglior giorno della settimana",
                        subtitle = "${weekBest.first.format(formatter)} • ${weekBest.second.toInt()} kcal risparmiate",
                        day = weekBest.first,
                        savedCalories = weekBest.second
                    )
                )
            }
        }

        return badges
    }

    private fun findBestDayInRange(
        start: LocalDate,
        end: LocalDate,
        mealByDate: Map<LocalDate, Double>,
        dailyBudget: Double
    ): Pair<LocalDate, Double>? {
        var date = start
        var bestDate: LocalDate? = null
        var bestSaving = Double.NEGATIVE_INFINITY

        while (!date.isAfter(end)) {
            val consumed = mealByDate[date] ?: 0.0
            if (consumed > 0.0) {
                val saving = dailyBudget - consumed
                if (saving > bestSaving) {
                    bestSaving = saving
                    bestDate = date
                }
            }
            date = date.plusDays(1)
        }

        return if (bestDate != null && bestSaving > 0.0) bestDate to bestSaving else null
    }

    private fun buildMotivationChallenges(
        month: YearMonth,
        meals: List<com.youandmedia.caldis.data.model.Meal>,
        dailyBudget: Double,
        burnedLast7: Double,
        zone: ZoneId
    ): List<MotivationChallenge> {
        val mealByDate = meals.groupBy { java.time.Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
            .mapValues { (_, dayMeals) -> dayMeals.sumOf { it.calories } }

        val today = LocalDate.now(zone)
        val effectiveEnd = if (month == YearMonth.now()) today.minusDays(1) else month.atEndOfMonth()
        val monthStart = month.atDay(1)

        var underBudgetDays = 0
        var streak = 0
        var date = monthStart
        while (!date.isAfter(effectiveEnd)) {
            val consumed = mealByDate[date] ?: 0.0
            if (consumed > 0.0 && consumed <= dailyBudget) {
                underBudgetDays++
            }
            date = date.plusDays(1)
        }

        date = effectiveEnd
        while (!date.isBefore(monthStart)) {
            val consumed = mealByDate[date] ?: 0.0
            if (consumed > 0.0 && consumed <= dailyBudget) {
                streak++
                date = date.minusDays(1)
            } else {
                break
            }
        }

        val monthTarget = 20f
        val monthlyProgress = (underBudgetDays / monthTarget).toFloat().coerceIn(0f, 1f)
        val weeklyBurnTarget = 1800.0
        val burnProgress = (burnedLast7 / weeklyBurnTarget).toFloat().coerceIn(0f, 1f)
        val streakTarget = 7f
        val streakProgress = (streak / streakTarget).toFloat().coerceIn(0f, 1f)

        return listOf(
            MotivationChallenge(
                title = "Sfida Costanza Mensile",
                subtitle = "Rimani sotto budget per 20 giorni nel mese",
                progress = monthlyProgress,
                progressLabel = "$underBudgetDays/20 giorni",
                isCompleted = underBudgetDays >= 20
            ),
            MotivationChallenge(
                title = "Sfida Attivita 7 Giorni",
                subtitle = "Brucia 1800 kcal in 7 giorni",
                progress = burnProgress,
                progressLabel = "${burnedLast7.toInt()}/1800 kcal",
                isCompleted = burnedLast7 >= weeklyBurnTarget
            ),
            MotivationChallenge(
                title = "Streak Sotto Budget",
                subtitle = "Mantieni 7 giorni consecutivi sotto budget",
                progress = streakProgress,
                progressLabel = "$streak/7 giorni",
                isCompleted = streak >= 7
            )
        )
    }

    private fun buildDailyMission(
        mealsLast7: List<com.youandmedia.caldis.data.model.Meal>,
        dailyBudget: Double,
        zone: ZoneId
    ): DailyMission? {
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)
        val dayTotals = mealsLast7.groupBy { java.time.Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
            .mapValues { (_, dayMeals) -> dayMeals.sumOf { it.calories } }

        val yesterdayCalories = dayTotals[yesterday] ?: return null
        val avg = if (dayTotals.isNotEmpty()) dayTotals.values.average() else dailyBudget
        val target = maxOf(0.0, minOf(dailyBudget, avg - 150.0))
        val completed = yesterdayCalories <= target

        return DailyMission(
            title = "Missione Giornaliera",
            subtitle = "Obiettivo ieri: <= ${target.toInt()} kcal (${if (completed) "completata" else "non completata"})",
            isCompleted = completed
        )
    }

    private fun buildMotivationProfile(
        mealCount: Int,
        activityCount: Int,
        badgesCount: Int,
        challenges: List<MotivationChallenge>
    ): MotivationProfile {
        val completedChallenges = challenges.count { it.isCompleted }
        val xp = mealCount * 10 + activityCount * 8 + badgesCount * 40 + completedChallenges * 120
        val level = (xp / 250) + 1
        val nextLevelXp = level * 250
        return MotivationProfile(
            xp = xp,
            level = level,
            nextLevelXp = nextLevelXp
        )
    }
}

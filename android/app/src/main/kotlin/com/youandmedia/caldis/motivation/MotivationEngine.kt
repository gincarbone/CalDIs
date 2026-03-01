package com.youandmedia.caldis.motivation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.youandmedia.caldis.CaldisApp
import com.youandmedia.caldis.R
import com.youandmedia.caldis.util.DEFAULT_DAILY_CALORIE_BUDGET
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

object MotivationEngine {
    private const val CHANNEL_ID = "motivation_badges"
    private const val CHANNEL_NAME = "Badge motivazionali"
    private const val CHANNEL_DESC = "Premi e riconoscimenti sui tuoi progressi"

    private const val KEY_LAST_WEEK_BADGE_DAY = "motivation_last_week_badge_day"
    private const val KEY_LAST_MONTH_BADGE_DAY = "motivation_last_month_badge_day"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = CHANNEL_DESC
        }
        manager.createNotificationChannel(channel)
    }

    suspend fun evaluateAndNotify(context: Context) {
        if (!canPostNotifications(context)) return

        val app = context.applicationContext as CaldisApp
        val prefs = context.getSharedPreferences("caldis_prefs", Context.MODE_PRIVATE)
        val dailyBudget = prefs.getFloat("daily_calorie_budget", DEFAULT_DAILY_CALORIE_BUDGET.toFloat()).toDouble()
        if (dailyBudget <= 0.0) return

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)
        val monthStart = yesterday.withDayOfMonth(1)

        val startMillis = monthStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val yesterdayEndMillis = yesterday.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        val meals = app.database.mealDao().getByDateRangeOnce(startMillis, yesterdayEndMillis)
        val caloriesByDay = meals
            .groupBy { java.time.Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
            .mapValues { (_, dayMeals) -> dayMeals.sumOf { it.calories } }

        val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale.ITALIAN)
        val yesterdayCalories = caloriesByDay[yesterday] ?: 0.0
        if (yesterdayCalories <= 0.0) return

        val yesterdaySaving = dailyBudget - yesterdayCalories
        if (yesterdaySaving <= 0.0) return

        val monthBest = findBestDayInRange(
            start = monthStart,
            end = yesterday,
            caloriesByDay = caloriesByDay,
            dailyBudget = dailyBudget
        )
        if (monthBest?.first == yesterday) {
            val alreadyNotified = prefs.getString(KEY_LAST_MONTH_BADGE_DAY, null)
            if (alreadyNotified != yesterday.toString()) {
                notifyBadge(
                    context = context,
                    title = "Badge Mensile Sbloccato",
                    message = "Il ${yesterday.format(formatter)} e il tuo miglior giorno del mese: ${yesterdaySaving.toInt()} kcal risparmiate.",
                    color = 0xFFFF8F00.toInt()
                )
                prefs.edit().putString(KEY_LAST_MONTH_BADGE_DAY, yesterday.toString()).apply()
            }
        }

        val weekIndex = (yesterday.dayOfMonth - 1) / 7
        val weekStart = monthStart.plusDays((weekIndex * 7).toLong())
        val weekBest = findBestDayInRange(
            start = weekStart,
            end = yesterday,
            caloriesByDay = caloriesByDay,
            dailyBudget = dailyBudget
        )
        if (weekBest?.first == yesterday) {
            val alreadyNotified = prefs.getString(KEY_LAST_WEEK_BADGE_DAY, null)
            if (alreadyNotified != yesterday.toString()) {
                notifyBadge(
                    context = context,
                    title = "Badge Settimanale Sbloccato",
                    message = "Il ${yesterday.format(formatter)} e il tuo miglior giorno della settimana: ${yesterdaySaving.toInt()} kcal risparmiate.",
                    color = 0xFF00ACC1.toInt()
                )
                prefs.edit().putString(KEY_LAST_WEEK_BADGE_DAY, yesterday.toString()).apply()
            }
        }
    }

    private fun notifyBadge(context: Context, title: String, message: String, color: Int) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(color)
            .setColorized(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(Random.nextInt(100_000, 999_999), notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun findBestDayInRange(
        start: LocalDate,
        end: LocalDate,
        caloriesByDay: Map<LocalDate, Double>,
        dailyBudget: Double
    ): Pair<LocalDate, Double>? {
        var date = start
        var bestDate: LocalDate? = null
        var bestSaving = Double.NEGATIVE_INFINITY

        while (!date.isAfter(end)) {
            val consumed = caloriesByDay[date] ?: 0.0
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
}

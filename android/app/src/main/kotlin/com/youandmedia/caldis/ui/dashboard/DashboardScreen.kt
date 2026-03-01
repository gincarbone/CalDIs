@file:OptIn(ExperimentalMaterial3Api::class)

package com.youandmedia.caldis.ui.dashboard

import android.app.Application
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youandmedia.caldis.CaldisApp
import com.youandmedia.caldis.data.model.FixedActivity
import com.youandmedia.caldis.data.model.FixedMeal
import com.youandmedia.caldis.ui.components.AddFixedEntryDialog
import com.youandmedia.caldis.ui.components.ConfirmDeleteDialog
import com.youandmedia.caldis.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RecentEntry(
    val id: Int,
    val category: String,
    val calories: Double,
    val description: String,
    val date: LocalDate,
    val isMeal: Boolean
)

data class DashboardUiState(
    val dailyCalorieBudget: Double = DEFAULT_DAILY_CALORIE_BUDGET,
    val totalFixedMealCalories: Double = 0.0,
    val totalFixedActivityCalories: Double = 0.0,
    val monthCalories: Double = 0.0,
    val monthBurned: Double = 0.0,
    val todayCalories: Double = 0.0,
    val todayBurned: Double = 0.0,
    val fixedMeals: List<FixedMeal> = emptyList(),
    val fixedActivities: List<FixedActivity> = emptyList(),
    val recentEntries: List<RecentEntry> = emptyList()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as CaldisApp).database
    private val prefs = application.getSharedPreferences("caldis_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            val month = YearMonth.now()
            val zone = ZoneId.systemDefault()
            val startOfMonth = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfMonth = month.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfDay = today.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

            val monthMeals = db.mealDao().getByDateRangeOnce(startOfMonth, endOfMonth)
            val monthActivities = db.activityDao().getByDateRangeOnce(startOfMonth, endOfMonth)
            val fixedMeals = db.fixedMealDao().getAllOnce()
            val fixedActivities = db.fixedActivityDao().getAllOnce()

            val savedBudget = prefs.getFloat("daily_calorie_budget", DEFAULT_DAILY_CALORIE_BUDGET.toFloat()).toDouble()

            val todayMeals = db.mealDao().getByDateRangeOnce(startOfDay, endOfDay)
            val todayActivities = db.activityDao().getByDateRangeOnce(startOfDay, endOfDay)

            val recentM = monthMeals.map {
                RecentEntry(it.id, it.category, it.calories, it.description,
                    Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate(), true)
            }
            val recentA = monthActivities.map {
                RecentEntry(it.id, it.category, it.caloriesBurned, it.description,
                    Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate(), false)
            }
            val recent = (recentM + recentA).sortedByDescending { it.date }.take(10)

            _uiState.value = DashboardUiState(
                dailyCalorieBudget = savedBudget,
                totalFixedMealCalories = fixedMeals.sumOf { it.calories },
                totalFixedActivityCalories = fixedActivities.sumOf { it.caloriesBurned },
                monthCalories = monthMeals.sumOf { it.calories },
                monthBurned = monthActivities.sumOf { it.caloriesBurned },
                todayCalories = todayMeals.sumOf { it.calories },
                todayBurned = todayActivities.sumOf { it.caloriesBurned },
                fixedMeals = fixedMeals,
                fixedActivities = fixedActivities,
                recentEntries = recent
            )
        }
    }

    fun addFixedMeal(category: String, amount: Double) {
        viewModelScope.launch { db.fixedMealDao().insert(FixedMeal(category = category, calories = amount)); loadData() }
    }
    fun addFixedActivity(category: String, amount: Double) {
        viewModelScope.launch { db.fixedActivityDao().insert(FixedActivity(category = category, caloriesBurned = amount)); loadData() }
    }
    fun deleteFixedMeal(item: FixedMeal) {
        viewModelScope.launch { db.fixedMealDao().delete(item); loadData() }
    }
    fun deleteFixedActivity(item: FixedActivity) {
        viewModelScope.launch { db.fixedActivityDao().delete(item); loadData() }
    }
    fun updateFixedMeal(item: FixedMeal, category: String, amount: Double) {
        viewModelScope.launch { db.fixedMealDao().update(item.copy(category = category, calories = amount)); loadData() }
    }
    fun updateFixedActivity(item: FixedActivity, category: String, amount: Double) {
        viewModelScope.launch { db.fixedActivityDao().update(item.copy(category = category, caloriesBurned = amount)); loadData() }
    }
}

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsState()
    val todayRemaining = state.dailyCalorieBudget - state.todayCalories + state.todayBurned

    var showAddMealDialog by remember { mutableStateOf(false) }
    var showAddActivityDialog by remember { mutableStateOf(false) }
    var editingFixedMeal by remember { mutableStateOf<FixedMeal?>(null) }
    var editingFixedActivity by remember { mutableStateOf<FixedActivity?>(null) }
    var deletingFixedMeal by remember { mutableStateOf<FixedMeal?>(null) }
    var deletingFixedActivity by remember { mutableStateOf<FixedActivity?>(null) }

    LaunchedEffect(Unit) { viewModel.loadData() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
        Spacer(modifier = Modifier.height(16.dp))

        // Balance card
        Surface(shape = RoundedCornerShape(24.dp), shadowElevation = 8.dp) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(GradientGreen, GradientTeal)))
                    .padding(24.dp)
            ) {
                Column {
                    Text("Riepilogo Giornaliero", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Text(
                        String.format("%.0f / %.0f kcal", state.todayCalories, state.dailyCalorieBudget),
                        color = if (todayRemaining >= 0) Color.White else Color(0xFFFF6B6B),
                        fontSize = 34.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MiniStat("Consumate mese", String.format("%.0f", state.monthCalories), Icons.Default.Restaurant, MealColor)
                        MiniStat("Bruciate mese", String.format("%.0f", state.monthBurned), Icons.Default.FitnessCenter, ActivityColor)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Budget cards row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BudgetCard("Budget", String.format("%.0f kcal", state.dailyCalorieBudget),
                Icons.Default.LocalFireDepartment, Color(0xFF2E7D32), Modifier.weight(1f))
            BudgetCard("Consumate Oggi", String.format("%.0f kcal", state.todayCalories),
                Icons.Default.Restaurant, if (state.todayCalories > state.dailyCalorieBudget) Color(0xFFFF6B6B) else MealColor, Modifier.weight(1f))
            BudgetCard("Rimangono Oggi", String.format("%.0f kcal", todayRemaining),
                Icons.Default.Battery5Bar, if (todayRemaining < 0) Color(0xFFFF6B6B) else Color(0xFF2ECC71), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Fixed Meals
        FixedSection(
            title = "Pasti Ricorrenti",
            icon = Icons.Default.Restaurant,
            iconColor = MealColor,
            items = state.fixedMeals.map { Triple(it.id, it.category, it.calories) },
            itemColor = MealColor,
            unit = "kcal",
            onAdd = { showAddMealDialog = true },
            onEdit = { id -> editingFixedMeal = state.fixedMeals.find { it.id == id }; showAddMealDialog = true },
            onDelete = { id -> deletingFixedMeal = state.fixedMeals.find { it.id == id } }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Fixed Activities
        FixedSection(
            title = "Attivit\u00E0 Ricorrenti",
            icon = Icons.Default.FitnessCenter,
            iconColor = ActivityColor,
            items = state.fixedActivities.map { Triple(it.id, it.category, it.caloriesBurned) },
            itemColor = ActivityColor,
            unit = "kcal",
            onAdd = { showAddActivityDialog = true },
            onEdit = { id -> editingFixedActivity = state.fixedActivities.find { it.id == id }; showAddActivityDialog = true },
            onDelete = { id -> deletingFixedActivity = state.fixedActivities.find { it.id == id } }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("Ultimi Movimenti", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
        Spacer(modifier = Modifier.height(8.dp))

        if (state.recentEntries.isEmpty()) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Text("Nessun movimento questo mese", color = Color.Gray, fontSize = 14.sp,
                    modifier = Modifier.padding(24.dp))
            }
        } else {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column {
                    state.recentEntries.forEachIndexed { index, entry ->
                        RecentEntryRow(entry)
                        if (index < state.recentEntries.lastIndex) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Dialogs
    if (showAddMealDialog) {
        AddFixedEntryDialog(
            isMeal = true, categories = constPastiFissi,
            initialCategory = editingFixedMeal?.category, initialAmount = editingFixedMeal?.calories?.toString() ?: "",
            onDismiss = { showAddMealDialog = false; editingFixedMeal = null },
            onSave = { cat, amt ->
                if (editingFixedMeal != null) viewModel.updateFixedMeal(editingFixedMeal!!, cat, amt)
                else viewModel.addFixedMeal(cat, amt)
                showAddMealDialog = false; editingFixedMeal = null
            }
        )
    }
    if (showAddActivityDialog) {
        AddFixedEntryDialog(
            isMeal = false, categories = constAttivitaFisse,
            initialCategory = editingFixedActivity?.category, initialAmount = editingFixedActivity?.caloriesBurned?.toString() ?: "",
            onDismiss = { showAddActivityDialog = false; editingFixedActivity = null },
            onSave = { cat, amt ->
                if (editingFixedActivity != null) viewModel.updateFixedActivity(editingFixedActivity!!, cat, amt)
                else viewModel.addFixedActivity(cat, amt)
                showAddActivityDialog = false; editingFixedActivity = null
            }
        )
    }
    deletingFixedMeal?.let { item ->
        ConfirmDeleteDialog(onDismiss = { deletingFixedMeal = null },
            onConfirm = { viewModel.deleteFixedMeal(item); deletingFixedMeal = null })
    }
    deletingFixedActivity?.let { item ->
        ConfirmDeleteDialog(onDismiss = { deletingFixedActivity = null },
            onConfirm = { viewModel.deleteFixedActivity(item); deletingFixedActivity = null })
    }
}

@Composable
fun MiniStat(label: String, value: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun BudgetCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
            Text(title, fontSize = 9.sp, color = Color.Gray)
        }
    }
}

@Composable
fun RecentEntryRow(entry: RecentEntry) {
    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(if (entry.isMeal) MealColor.copy(alpha = 0.15f) else ActivityColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Icon(if (entry.isMeal) Icons.Default.Restaurant else Icons.Default.FitnessCenter, null,
                tint = if (entry.isMeal) MealColor else ActivityColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.category, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C3E50))
            Text(entry.date.format(fmt), fontSize = 11.sp, color = Color.Gray)
        }
        Text(
            String.format("%s%.0f kcal", if (entry.isMeal) "+" else "-", entry.calories),
            fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = if (entry.isMeal) MealColor else ActivityColor
        )
    }
}

@Composable
fun FixedSection(
    title: String, icon: ImageVector, iconColor: Color,
    items: List<Triple<Int, String, Double>>, itemColor: Color,
    unit: String = "kcal",
    onAdd: () -> Unit, onEdit: (Int) -> Unit, onDelete: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
        Column {
            Surface(onClick = { expanded = !expanded }, color = Color.Transparent) {
                ListItem(
                    headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
                    leadingContent = { Icon(icon, null, tint = iconColor) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(String.format("%.0f %s", items.sumOf { it.third }, unit), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = iconColor)
                            Spacer(Modifier.width(4.dp))
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                        }
                    }
                )
            }
            if (expanded) {
                Surface(onClick = onAdd, color = Color.Transparent) {
                    ListItem(headlineContent = { Text("Aggiungi") }, leadingContent = { Icon(Icons.Default.Add, null) })
                }
                items.forEach { (id, cat, amt) ->
                    Surface(onClick = { onEdit(id) }) {
                        ListItem(
                            headlineContent = { Text("$cat: ${String.format("%.0f", amt)} $unit", color = Color.White, fontSize = 12.sp) },
                            leadingContent = { IconButton(onClick = { onDelete(id) }) { Icon(Icons.Outlined.DeleteOutline, "Elimina", tint = Color.White) } },
                            colors = ListItemDefaults.colors(containerColor = itemColor)
                        )
                    }
                }
            }
        }
    }
}

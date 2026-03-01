package com.youandmedia.caldis.ui.daydetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandmedia.caldis.data.model.Activity
import com.youandmedia.caldis.data.model.Meal
import com.youandmedia.caldis.ui.components.AddEditDialog
import com.youandmedia.caldis.ui.components.ConfirmDeleteDialog
import com.youandmedia.caldis.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: LocalDate,
    viewModel: DayDetailViewModel,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale.ITALIAN)

    var showMealDialog by remember { mutableStateOf(false) }
    var showActivityDialog by remember { mutableStateOf(false) }
    var editingMeal by remember { mutableStateOf<Meal?>(null) }
    var editingActivity by remember { mutableStateOf<Activity?>(null) }
    var deletingMeal by remember { mutableStateOf<Meal?>(null) }
    var deletingActivity by remember { mutableStateOf<Activity?>(null) }

    LaunchedEffect(date) {
        viewModel.loadDay(date)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(date.format(formatter)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        containerColor = BgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Single scrollable content list: meals + activities
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                if (state.meals.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pasti",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF2C3E50)
                        )
                    }
                }
                items(state.meals) { meal ->
                    MealItem(
                        meal = meal,
                        onDelete = { deletingMeal = meal },
                        onEdit = {
                            editingMeal = meal
                            showMealDialog = true
                        }
                    )
                    HorizontalDivider()
                }

                if (state.activities.isNotEmpty()) {
                    item {
                        Text(
                            text = "Attivita'",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF2C3E50)
                        )
                    }
                }
                items(state.activities) { activity ->
                    ActivityItem(
                        activity = activity,
                        onDelete = { deletingActivity = activity },
                        onEdit = {
                            editingActivity = activity
                            showActivityDialog = true
                        }
                    )
                    HorizontalDivider()
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        editingActivity = null
                        showActivityDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = ActivityColor, modifier = Modifier.size(40.dp))
                }

                Button(
                    onClick = onOpenCamera,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = GradientTeal, modifier = Modifier.size(40.dp))
                }

                Button(
                    onClick = {
                        editingMeal = null
                        showMealDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = MealColor, modifier = Modifier.size(40.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Meal dialog
    if (showMealDialog) {
        AddEditDialog(
            title = "Aggiungi Pasto",
            categories = constTipoPasti,
            initialCategory = editingMeal?.category,
            initialAmount = editingMeal?.calories?.toString() ?: "",
            initialNotes = editingMeal?.description ?: "",
            amountColor = MealColor,
            amountLabel = "Calorie (kcal)",
            onDismiss = { showMealDialog = false; editingMeal = null },
            onConfirm = { category, calories, notes ->
                if (editingMeal != null) {
                    viewModel.updateMeal(editingMeal!!, calories, category, notes, date)
                } else {
                    viewModel.saveMeal(calories, category, notes, date)
                }
                showMealDialog = false
                editingMeal = null
            }
        )
    }

    // Activity dialog
    if (showActivityDialog) {
        AddEditDialog(
            title = "Aggiungi Attivit\u00E0",
            categories = constTipoAttivita,
            initialCategory = editingActivity?.category,
            initialAmount = editingActivity?.caloriesBurned?.toString() ?: "",
            initialNotes = editingActivity?.description ?: "",
            amountColor = ActivityColor,
            amountLabel = "Calorie bruciate (kcal)",
            onDismiss = { showActivityDialog = false; editingActivity = null },
            onConfirm = { category, calories, notes ->
                if (editingActivity != null) {
                    viewModel.updateActivity(editingActivity!!, calories, category, notes, date)
                } else {
                    viewModel.saveActivity(calories, category, notes, date)
                }
                showActivityDialog = false
                editingActivity = null
            }
        )
    }

    deletingMeal?.let { meal ->
        ConfirmDeleteDialog(
            onDismiss = { deletingMeal = null },
            onConfirm = { viewModel.deleteMeal(meal, date); deletingMeal = null }
        )
    }

    deletingActivity?.let { activity ->
        ConfirmDeleteDialog(
            onDismiss = { deletingActivity = null },
            onConfirm = { viewModel.deleteActivity(activity, date); deletingActivity = null }
        )
    }
}

@Composable
fun MealItem(meal: Meal, onDelete: () -> Unit, onEdit: () -> Unit) {
    ListItem(
        headlineContent = { Text(meal.category, fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color(0xDD000000)) },
        supportingContent = { Text(meal.description, fontSize = 11.sp, color = Color.Black) },
        leadingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Elimina", tint = Color.Gray)
            }
        },
        trailingContent = {
            Text(String.format("+%.0f kcal", meal.calories), fontSize = 18.sp, color = MealColor)
        },
        colors = ListItemDefaults.colors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().clickable { onEdit() }
    )
}

@Composable
fun ActivityItem(activity: Activity, onDelete: () -> Unit, onEdit: () -> Unit) {
    ListItem(
        headlineContent = { Text(activity.category, fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color(0xDD000000)) },
        supportingContent = { Text(activity.description, fontSize = 11.sp, color = Color.Black) },
        leadingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Elimina", tint = Color.Gray)
            }
        },
        trailingContent = {
            Text(String.format("-%.0f kcal", activity.caloriesBurned), fontSize = 18.sp, color = ActivityColor)
        },
        colors = ListItemDefaults.colors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().clickable { onEdit() }
    )
}

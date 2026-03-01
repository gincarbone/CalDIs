package com.youandmedia.caldis.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandmedia.caldis.ui.components.GaugeWidget
import com.youandmedia.caldis.util.*
import kotlin.math.abs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onDaySelected: (LocalDate) -> Unit,
    onAddMeal: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.selectedMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.ITALIAN)
                            .replaceFirstChar { it.uppercase() }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMeal,
                containerColor = MealColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Aggiungi pasto da fotocamera")
            }
        },
        containerColor = BgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            CalendarGrid(
                yearMonth = state.selectedMonth,
                datesWithMeals = state.datesWithMeals,
                datesWithActivities = state.datesWithActivities,
                dailyCalories = state.dailyCalories,
                dailyBudget = state.dailyCalorieBudget,
                onDaySelected = onDaySelected,
                onMonthChanged = { viewModel.onMonthChanged(it) }
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Gauge
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                val today = LocalDate.now()
                val todayCalories = state.dailyCalories[today] ?: 0.0
                val gaugeValue = if (state.dailyCalorieBudget > 0) {
                    (todayCalories / state.dailyCalorieBudget).coerceIn(0.0, 1.0)
                } else 0.0

                GaugeWidget(
                    value = gaugeValue,
                    budgetGiornaliero = state.dailyCalorieBudget,
                    consumateOggi = todayCalories,
                    bruciateOggi = 0.0,
                    pastiRicorrenti = state.totalFixedMealCalories
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            if (viewModel.isCurrentMonth()) {
                ForecastCard(state)
            } else {
                PastMonthCard(state)
            }

            Spacer(modifier = Modifier.height(screenHeight * 0.15f))
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    isMeal: Boolean,
    onToggleType: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String) -> Unit
) {
    val categories = if (isMeal) constTipoPasti else constTipoAttivita
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(isMeal) {
        selectedCategory = (if (isMeal) constTipoPasti else constTipoAttivita).first()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Aggiungi Veloce",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isMeal,
                    onClick = { if (!isMeal) onToggleType() },
                    label = { Text("Pasto") },
                    leadingIcon = if (isMeal) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MealColor,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isMeal,
                    onClick = { if (isMeal) onToggleType() },
                    label = { Text("Attivit\u00E0") },
                    leadingIcon = if (!isMeal) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ActivityColor,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.replace(',', '.') },
                label = { Text(if (isMeal) "Calorie (kcal)" else "Calorie bruciate (kcal)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMeal) MealColor else ActivityColor,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoria") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notesText,
                onValueChange = { if (it.length <= 24) notesText = it },
                label = { Text("Note (opzionale)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                supportingText = { Text("${notesText.length}/24") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onConfirm(amount, selectedCategory, notesText)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMeal) MealColor else ActivityColor
                )
            ) {
                Text(
                    if (isMeal) "Aggiungi Pasto" else "Aggiungi Attivit\u00E0",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    datesWithMeals: Set<LocalDate>,
    datesWithActivities: Set<LocalDate>,
    dailyCalories: Map<LocalDate, Double>,
    dailyBudget: Double,
    onDaySelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit
) {
    val today = LocalDate.now()
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value - 1)

    val dayNames = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        shape = RoundedCornerShape(30.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChanged(yearMonth.minusMonths(1)) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mese precedente")
                }
                Text(
                    "${yearMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.ITALIAN).replaceFirstChar { it.uppercase() }} ${yearMonth.year}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = { onMonthChanged(yearMonth.plusMonths(1)) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Mese successivo")
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                dayNames.forEachIndexed { index, name ->
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (index >= 5) 12.sp else 15.sp,
                        color = if (index >= 5) Color(0xFFFF5252) else Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            var dayCounter = 1
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        if (cellIndex >= firstDayOfWeek && dayCounter <= daysInMonth) {
                            val date = yearMonth.atDay(dayCounter)
                            val isToday = date == today
                            val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                            val hasMeal = datesWithMeals.contains(date)
                            val hasActivity = datesWithActivities.contains(date)

                            val dayCalories = dailyCalories[date] ?: 0.0
                            val budgetColor = if (hasMeal && dailyBudget > 0) {
                                val ratio = dayCalories / dailyBudget
                                when {
                                    ratio <= 0.7 -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    ratio <= 1.0 -> Color(0xFFFFC107).copy(alpha = 0.15f)
                                    else -> Color(0xFFF44336).copy(alpha = 0.15f)
                                }
                            } else Color.Transparent

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(budgetColor)
                                    .clickable { onDaySelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$dayCounter",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isToday -> Color(0xFF2E7D32)
                                        isWeekend -> Color(0xFFFF5252)
                                        else -> Color.Black
                                    }
                                )

                                if (hasMeal) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 12.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MealColor)
                                    )
                                }

                                if (hasActivity) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 12.dp, end = 5.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(ActivityColor)
                                    )
                                }
                            }
                            dayCounter++
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastCard(state: CalendarUiState) {
    val monthlyBudget = state.dailyCalorieBudget * state.selectedMonth.lengthOfMonth()
    val savedCalories = monthlyBudget - state.forecastCaloriesOfMonth
    val estimatedKgChange = savedCalories / 7700.0
    val isWeightLoss = estimatedKgChange >= 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .height(300.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(colors = listOf(GradientGreen, GradientTeal))
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Previsioni Calorie",
                    color = Color.White,
                    fontSize = 18.sp
                )

                Text(
                    "Le previsioni sono calcolate da un algoritmo sulla base delle tue abitudini alimentari rilevate durante il mese in corso.",
                    color = Color.White,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    if (isWeightLoss) String.format("-%.2f kg", estimatedKgChange) else String.format("+%.2f kg", abs(estimatedKgChange)),
                    color = if (isWeightLoss) Color.White else Color(0xFFFFCDD2),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    if (isWeightLoss) "perdita stimata a fine mese" else "aumento stimato a fine mese",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(15.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ForecastColumn(
                        icon = Icons.Default.LocalFireDepartment,
                        label = "Perdita kg fine mese*",
                        value = if (isWeightLoss) String.format("%.2f kg", estimatedKgChange) else String.format("-%.2f kg", abs(estimatedKgChange))
                    )
                    ForecastColumn(
                        icon = Icons.Default.GraphicEq,
                        label = "Calorie risparmiate*",
                        value = String.format("%.0f kcal", savedCalories)
                    )
                    ForecastColumn(
                        icon = Icons.Default.Lightbulb,
                        label = "Budget giornaliero",
                        value = String.format("%.0f kcal", state.dailyCalorieBudget)
                    )
                }
            }
        }
    }
}

@Composable
fun ForecastColumn(icon: ImageVector, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.padding(8.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun PastMonthCard(state: CalendarUiState) {
    val monthlyBudget = state.dailyCalorieBudget * state.selectedMonth.lengthOfMonth()
    val diff = monthlyBudget - state.totalCaloriesOfMonth
    val isUnder = diff >= 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .height(250.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Riepilogo ${state.selectedMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.ITALIAN)}",
                color = Color.Gray,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                String.format("%.0f kcal", state.totalCaloriesOfMonth),
                color = Color(0xDD000000),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )

            Icon(
                imageVector = if (isUnder) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                contentDescription = null,
                tint = if (isUnder) ActivityColor else MealColor,
                modifier = Modifier.size(80.dp)
            )

            Text(
                if (isUnder) "Mese chiuso. Ottimo lavoro!" else "Hai superato il budget calorico!",
                color = Color.Black,
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

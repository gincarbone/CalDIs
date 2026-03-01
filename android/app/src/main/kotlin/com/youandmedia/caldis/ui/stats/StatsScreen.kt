package com.youandmedia.caldis.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandmedia.caldis.util.BgColor
import com.youandmedia.caldis.util.GradientGreen
import com.youandmedia.caldis.util.GradientTeal
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val chartColors = listOf(
    Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFF45B7D1),
    Color(0xFFFFA07A), Color(0xFF98D8C8), Color(0xFFF7DC6F),
    Color(0xFFBB8FCE), Color(0xFF85C1E9), Color(0xFFE74C3C),
    Color(0xFF2ECC71), Color(0xFF3498DB), Color(0xFFF39C12),
    Color(0xFF1ABC9C)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiche") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = BgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            MonthSelector(state = state, onPrev = { viewModel.prevMonth() }, onNext = { viewModel.nextMonth() })
            SummaryHeaderCard(state)
            Spacer(modifier = Modifier.height(16.dp))
            QuickStatsRow(state)
            Spacer(modifier = Modifier.height(20.dp))

            if (state.weeklyTrends.isNotEmpty()) {
                Text("Settimanale", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFF2C3E50))
                Spacer(modifier = Modifier.height(12.dp))
                TrendChart(
                    trends = state.weeklyTrends,
                    thresholdValue = state.dailyBudget * 7.0,
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 16.dp)
                )

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFFF6B6B)))
                    Spacer(Modifier.width(4.dp))
                    Text("Calorie", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.width(16.dp))
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF4ECDC4)))
                    Spacer(Modifier.width(4.dp))
                    Text("Bruciate", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.width(16.dp))
                    Canvas(modifier = Modifier.width(16.dp).height(12.dp)) {
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.7f),
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("Soglia", fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (state.dailyTrendsLast7.isNotEmpty()) {
                Text("Giornaliero", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFF2C3E50))
                Spacer(modifier = Modifier.height(12.dp))
                TrendChart(
                    trends = state.dailyTrendsLast7,
                    thresholdValue = state.dailyBudget,
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (state.categoryStats.isNotEmpty()) {
                Text("Ripartizione Calorie", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFF2C3E50))
                Spacer(modifier = Modifier.height(12.dp))
                AnimatedDonutChart(stats = state.categoryStats, totalCalories = state.totalCalories,
                    modifier = Modifier.fillMaxWidth().height(260.dp).padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(20.dp))

                Text("Dettaglio Categorie", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFF2C3E50))
                Spacer(modifier = Modifier.height(12.dp))

                state.categoryStats.forEachIndexed { index, stat ->
                    CategoryCard(stat = stat, color = chartColors[index % chartColors.size],
                        maxCalories = state.categoryStats.first().calories)
                }
            } else {
                EmptyState()
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun TrendChart(
    trends: List<PeriodTrend>,
    thresholdValue: Double,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(trends) {
        animProgress.snapTo(0f)
        animProgress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing))
    }

    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (trends.isEmpty()) return@Canvas
            val rawMax = maxOf(
                trends.maxOf { maxOf(it.totalCalories, it.totalBurned) },
                thresholdValue
            )
            if (rawMax <= 0) return@Canvas
            // Keep the threshold line slightly lower in the chart area.
            val maxVal = rawMax * 1.2

            val barGroupWidth = size.width / trends.size
            val barWidth = barGroupWidth * 0.3f
            val gap = barWidth * 0.15f
            val chartHeight = size.height - 42.dp.toPx()
            val thresholdY = if (thresholdValue > 0) {
                chartHeight - (thresholdValue / maxVal * chartHeight).toFloat()
            } else {
                chartHeight
            }

            if (thresholdValue > 0) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.7f),
                    start = Offset(0f, thresholdY),
                    end = Offset(size.width, thresholdY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }

            trends.forEachIndexed { index, trend ->
                val centerX = barGroupWidth * index + barGroupWidth / 2

                val calHeight = (trend.totalCalories / maxVal * chartHeight * animProgress.value).toFloat()
                val calTop = chartHeight - calHeight
                drawRoundRect(color = Color(0xFFFF6B6B), topLeft = Offset(centerX - barWidth - gap / 2, calTop),
                    size = Size(barWidth, calHeight), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))

                val burnHeight = (trend.totalBurned / maxVal * chartHeight * animProgress.value).toFloat()
                drawRoundRect(color = Color(0xFF4ECDC4), topLeft = Offset(centerX + gap / 2, chartHeight - burnHeight),
                    size = Size(barWidth, burnHeight), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))

                // Motivation marker:
                // place smiley above threshold line only when there is real intake and calories are under target.
                if (thresholdValue > 0 && trend.totalCalories > 0.0 && trend.totalCalories < thresholdValue) {
                    val topSpace = thresholdY
                    if (topSpace > 18.dp.toPx()) {
                        val faceRadius = minOf(topSpace * 0.18f, barWidth * 0.45f).coerceAtLeast(5.dp.toPx())
                        val faceCenter = Offset(centerX - barWidth / 2 - gap / 2, thresholdY * 0.5f)
                        drawCircle(color = Color(0xFFFFD54F), radius = faceRadius, center = faceCenter)
                        drawCircle(color = Color(0xFF5D4037), radius = faceRadius * 0.12f, center = faceCenter + Offset(-faceRadius * 0.28f, -faceRadius * 0.18f))
                        drawCircle(color = Color(0xFF5D4037), radius = faceRadius * 0.12f, center = faceCenter + Offset(faceRadius * 0.28f, -faceRadius * 0.18f))
                        drawArc(
                            color = Color(0xFF5D4037),
                            startAngle = 20f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(faceCenter.x - faceRadius * 0.42f, faceCenter.y - faceRadius * 0.18f),
                            size = Size(faceRadius * 0.84f, faceRadius * 0.84f),
                            style = Stroke(width = 1.3.dp.toPx())
                        )
                    }
                }

                val labelResult = textMeasurer.measure(text = trend.label, style = TextStyle(color = Color.Gray, fontSize = 10.sp))
                drawText(textMeasurer = textMeasurer, text = trend.label, style = TextStyle(color = Color.Gray, fontSize = 10.sp),
                    topLeft = Offset(centerX - labelResult.size.width / 2, chartHeight + 6.dp.toPx()))
            }

            if (thresholdValue > 0) {
                val thresholdLabel = "Soglia: ${thresholdValue.toInt()} kcal"
                val thresholdLabelResult = textMeasurer.measure(
                    text = thresholdLabel,
                    style = TextStyle(color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = thresholdLabel,
                    style = TextStyle(color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                    topLeft = Offset(size.width - thresholdLabelResult.size.width, chartHeight + 22.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun MonthSelector(state: StatsUiState, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, contentDescription = "Mese precedente", tint = GradientTeal) }
        Text("${state.selectedMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.ITALIAN).replaceFirstChar { it.uppercase() }} ${state.selectedMonth.year}",
            fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C3E50))
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, contentDescription = "Mese successivo", tint = GradientTeal) }
    }
}

@Composable
fun SummaryHeaderCard(state: StatsUiState) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp), shadowElevation = 8.dp) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(colors = listOf(GradientGreen, GradientTeal))).padding(24.dp)) {
            Column {
                Text("Calorie Totali", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(String.format("%.0f kcal", state.totalCalories), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SummaryMiniStat("Bruciate", String.format("%.0f kcal", state.totalBurned), Icons.Default.FitnessCenter, Color(0xFF4ECDC4))
                    SummaryMiniStat("Bilancio", String.format("%.0f kcal", state.totalCalories - state.totalBurned), Icons.Default.Balance, Color(0xFFF7DC6F))
                }
            }
        }
    }
}

@Composable
fun SummaryMiniStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun QuickStatsRow(state: StatsUiState) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickStatCard("Media/giorno", String.format("%.0f", state.avgDailyCalories), Icons.Default.CalendarToday, Color(0xFF45B7D1), Modifier.weight(1f))
        QuickStatCard("Pasti", "${state.entryCount}", Icons.Default.Restaurant, Color(0xFFBB8FCE), Modifier.weight(1f))
        QuickStatCard("Top categoria", state.topCategory, Icons.Default.Star, Color(0xFFF7DC6F), Modifier.weight(1f))
    }
}

@Composable
fun QuickStatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun AnimatedDonutChart(stats: List<CategoryStat>, totalCalories: Double, modifier: Modifier = Modifier) {
    val animProgress = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(stats) {
        animProgress.snapTo(0f)
        animProgress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier) {
        val canvasSize = minOf(size.width, size.height)
        val strokeWidth = 40.dp.toPx()
        val radius = canvasSize / 2 - strokeWidth / 2 - 10.dp.toPx()
        val centerX = size.width / 2
        val centerY = size.height / 2
        val arcRect = Size(radius * 2, radius * 2)
        val topLeft = Offset(centerX - radius, centerY - radius)

        drawArc(color = Color(0xFFF0F0F0), startAngle = 0f, sweepAngle = 360f, useCenter = false,
            topLeft = topLeft, size = arcRect, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))

        var startAngle = -90f
        val total = stats.sumOf { it.calories }
        if (total > 0) {
            stats.forEachIndexed { index, stat ->
                val sweepAngle = (stat.calories / total * 360).toFloat() * animProgress.value
                val color = chartColors[index % chartColors.size]
                drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle.coerceAtLeast(0.5f), useCenter = false,
                    topLeft = topLeft, size = arcRect, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
                startAngle += sweepAngle
            }
        }

        startAngle = -90f
        if (stats.size > 1 && total > 0) {
            stats.forEach { stat ->
                val sweepAngle = (stat.calories / total * 360).toFloat() * animProgress.value
                startAngle += sweepAngle
                val angleRad = Math.toRadians(startAngle.toDouble())
                val innerR = radius - strokeWidth / 2
                val outerR = radius + strokeWidth / 2
                drawLine(color = Color.White,
                    start = Offset(centerX + (innerR * kotlin.math.cos(angleRad)).toFloat(), centerY + (innerR * kotlin.math.sin(angleRad)).toFloat()),
                    end = Offset(centerX + (outerR * kotlin.math.cos(angleRad)).toFloat(), centerY + (outerR * kotlin.math.sin(angleRad)).toFloat()),
                    strokeWidth = 3.dp.toPx())
            }
        }

        val totalText = String.format("%.0f", totalCalories)
        val totalResult = textMeasurer.measure(text = totalText, style = TextStyle(color = Color(0xFF2C3E50), fontSize = 24.sp, fontWeight = FontWeight.Bold))
        drawText(textMeasurer = textMeasurer, text = totalText, style = TextStyle(color = Color(0xFF2C3E50), fontSize = 24.sp, fontWeight = FontWeight.Bold),
            topLeft = Offset(centerX - totalResult.size.width / 2, centerY - totalResult.size.height / 2 - 8.dp.toPx()))

        val labelResult = textMeasurer.measure(text = "kcal", style = TextStyle(color = Color.Gray, fontSize = 12.sp))
        drawText(textMeasurer = textMeasurer, text = "kcal", style = TextStyle(color = Color.Gray, fontSize = 12.sp),
            topLeft = Offset(centerX - labelResult.size.width / 2, centerY + totalResult.size.height / 2 - 6.dp.toPx()))
    }
}

@Composable
fun CategoryCard(stat: CategoryStat, color: Color, maxCalories: Double) {
    val barProgress = remember { Animatable(0f) }
    LaunchedEffect(stat) {
        barProgress.snapTo(0f)
        barProgress.animateTo(targetValue = if (maxCalories > 0) (stat.calories / maxCalories).toFloat() else 0f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing))
    }

    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stat.category, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C3E50))
                    Text(String.format("%.0f kcal", stat.calories), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFF0F0F0))) {
                    Box(modifier = Modifier.fillMaxWidth(barProgress.value).fillMaxHeight().clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(colors = listOf(color.copy(alpha = 0.7f), color))))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${stat.entryCount} ${if (stat.entryCount == 1) "pasto" else "pasti"}", fontSize = 11.sp, color = Color.Gray)
                    Text(String.format("%.1f%%", stat.percentage), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(modifier = Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.InsertChart, contentDescription = null, tint = Color(0xFFBDC3C7), modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Nessun pasto registrato", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7F8C8D))
        Text("Le statistiche appariranno quando\naggiungerai dei pasti", fontSize = 13.sp, color = Color(0xFFBDC3C7),
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    }
}

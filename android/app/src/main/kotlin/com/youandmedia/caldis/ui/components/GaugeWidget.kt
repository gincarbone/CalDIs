package com.youandmedia.caldis.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@OptIn(ExperimentalTextApi::class)
@Composable
fun GaugeWidget(
    value: Double,
    budgetGiornaliero: Double,
    consumateOggi: Double,
    bruciateOggi: Double,
    pastiRicorrenti: Double,
    modifier: Modifier = Modifier
) {
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(value) {
        animatedValue.animateTo(
            targetValue = value.toFloat(),
            animationSpec = tween(durationMillis = 2000)
        )
    }

    val residuo = max(0.0, budgetGiornaliero - consumateOggi + bruciateOggi)
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(10.dp)
    ) {
        val centerX = size.width / 2 + 50.dp.toPx()
        val centerY = size.height / 2
        val arcSize = 170.dp.toPx()
        val strokeWidth = 20.dp.toPx()

        // Title
        val titleResult = textMeasurer.measure(
            text = "Calorie",
            style = TextStyle(
                color = Color(0x89000000),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )
        )
        drawText(
            textLayoutResult = titleResult,
            topLeft = Offset(
                (size.width - titleResult.size.width) / 2 - 120.dp.toPx(),
                (size.height - titleResult.size.height) / 2 - 70.dp.toPx()
            )
        )

        // Amount text
        val amountText = String.format("%.0f", residuo)
        val amountResult = textMeasurer.measure(
            text = "${amountText} kcal",
            style = TextStyle(
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )
        drawText(
            textLayoutResult = amountResult,
            topLeft = Offset(
                centerX - amountResult.size.width / 2,
                centerY - amountResult.size.height / 2
            )
        )

        // Background arc
        drawArc(
            color = Color(0x1F000000),
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(centerX - arcSize / 2, centerY - arcSize / 2),
            size = Size(arcSize, arcSize),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Progress arc
        drawArc(
            color = Color(0xFF2E7D32),
            startAngle = 140f,
            sweepAngle = 260f * animatedValue.value,
            useCenter = false,
            topLeft = Offset(centerX - arcSize / 2, centerY - arcSize / 2),
            size = Size(arcSize, arcSize),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Side info items
        val items = listOf(
            Triple("${budgetGiornaliero.toInt()}", "Budget giornaliero", Color(0xFF2E7D32)),
            Triple("${consumateOggi.toInt()}", "Consumate oggi", Color(0xFFFF8A65)),
            Triple("${bruciateOggi.toInt()}", "Bruciate oggi", Color(0xFF66BB6A)),
            Triple("${pastiRicorrenti.toInt()}", "Pasti ricorrenti", Color(0xFF00838F))
        )

        val baseX = size.width - 330.dp.toPx()
        val spaceBetween = 10.dp.toPx()

        items.forEachIndexed { index, (amount, description, color) ->
            val y = size.height - (128 - index * 39).dp.toPx() - spaceBetween

            // Colored dot
            drawCircle(color = color, radius = 5.dp.toPx(), center = Offset(baseX, y))

            // Amount text
            val amtResult = textMeasurer.measure(
                text = amount,
                style = TextStyle(color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            )
            drawText(textLayoutResult = amtResult, topLeft = Offset(baseX + 15.dp.toPx(), y - 8.dp.toPx()))

            // Description text
            val descResult = textMeasurer.measure(
                text = description,
                style = TextStyle(color = Color.Black, fontSize = 10.sp)
            )
            drawText(textLayoutResult = descResult, topLeft = Offset(baseX + 15.dp.toPx(), y + 11.dp.toPx()))
        }
    }
}

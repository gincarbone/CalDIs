package com.youandmedia.caldis.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandmedia.caldis.util.GradientGreen
import com.youandmedia.caldis.util.GradientTeal
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(4000)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(GradientGreen, GradientTeal)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Outlined.Restaurant,
                contentDescription = "CalDis",
                tint = Color.White,
                modifier = Modifier.size(100.dp)
            )

            Text(
                text = "CalDis",
                color = Color.White,
                fontSize = 14.sp
            )

            Text(
                text = "Il tuo diario alimentare",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("You&Media", color = Color.White, fontSize = 10.sp)
                Text("v. 1.0.0", color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

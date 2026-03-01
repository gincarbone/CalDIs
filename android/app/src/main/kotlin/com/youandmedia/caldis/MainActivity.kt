package com.youandmedia.caldis

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.youandmedia.caldis.ai.GeminiService
import com.youandmedia.caldis.motivation.MotivationEngine
import com.youandmedia.caldis.navigation.NavGraph
import com.youandmedia.caldis.navigation.Routes
import com.youandmedia.caldis.navigation.bottomNavRoutes
import com.youandmedia.caldis.ui.calendar.CalendarViewModel
import com.youandmedia.caldis.ui.dashboard.DashboardViewModel
import com.youandmedia.caldis.ui.theme.CaldisTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        MotivationEngine.ensureChannel(this)
        lifecycleScope.launch(Dispatchers.IO) {
            MotivationEngine.evaluateAndNotify(this@MainActivity)
        }

        // Initialize Gemini with saved API key
        val prefs = getSharedPreferences("caldis_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
        val modelName = prefs.getString("gemini_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        if (apiKey.isNotBlank()) {
            GeminiService.initialize(apiKey, modelName)
        }

        setContent {
            CaldisTheme {
                val navController = rememberNavController()
                val calendarViewModel: CalendarViewModel = viewModel()
                val dashboardViewModel: DashboardViewModel = viewModel()

                val navItems = listOf(
                    BottomNavItem(Routes.CALENDAR, "Calendario", Icons.Default.CalendarMonth),
                    BottomNavItem(Routes.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
                    BottomNavItem(Routes.STATS, "Statistiche", Icons.Default.BarChart),
                    BottomNavItem(Routes.SETTINGS, "Impostazioni", Icons.Default.Settings)
                )

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in bottomNavRoutes

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                navItems.forEach { item ->
                                    NavigationBarItem(
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label, maxLines = 1) },
                                        selected = currentRoute == item.route,
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    NavGraph(
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        dashboardViewModel = dashboardViewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }
}

package com.youandmedia.caldis.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.youandmedia.caldis.ui.calendar.CalendarScreen
import com.youandmedia.caldis.ui.calendar.CalendarViewModel
import com.youandmedia.caldis.ui.dashboard.DashboardScreen
import com.youandmedia.caldis.ui.dashboard.DashboardViewModel
import com.youandmedia.caldis.ui.daydetail.DayDetailScreen
import com.youandmedia.caldis.ui.daydetail.DayDetailViewModel
import com.youandmedia.caldis.ui.photo.PhotoEstimateScreen
import com.youandmedia.caldis.ui.photo.PhotoEstimateViewModel
import com.youandmedia.caldis.ui.settings.SettingsScreen
import com.youandmedia.caldis.ui.splash.SplashScreen
import com.youandmedia.caldis.ui.stats.StatsScreen
import com.youandmedia.caldis.ui.stats.StatsViewModel
import java.time.LocalDate

object Routes {
    const val SPLASH = "splash"
    const val CALENDAR = "calendar"
    const val DASHBOARD = "dashboard"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val DAY_DETAIL = "day_detail/{year}/{month}/{day}"
    const val PHOTO_ESTIMATE = "photo_estimate/{year}/{month}/{day}?autoCamera={autoCamera}"

    fun dayDetail(date: LocalDate): String =
        "day_detail/${date.year}/${date.monthValue}/${date.dayOfMonth}"

    fun photoEstimate(date: LocalDate, autoCamera: Boolean = false): String =
        "photo_estimate/${date.year}/${date.monthValue}/${date.dayOfMonth}?autoCamera=$autoCamera"
}

val bottomNavRoutes = listOf(Routes.CALENDAR, Routes.DASHBOARD, Routes.STATS, Routes.SETTINGS)

@Composable
fun NavGraph(
    navController: NavHostController,
    calendarViewModel: CalendarViewModel,
    dashboardViewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = Routes.SPLASH, modifier = modifier) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Routes.CALENDAR) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CALENDAR) {
            CalendarScreen(
                viewModel = calendarViewModel,
                onDaySelected = { date ->
                    navController.navigate(Routes.dayDetail(date))
                },
                onAddMeal = {
                    navController.navigate(Routes.photoEstimate(LocalDate.now(), autoCamera = true))
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(viewModel = dashboardViewModel)
        }

        composable(Routes.STATS) {
            val statsViewModel: StatsViewModel = viewModel()
            StatsScreen(viewModel = statsViewModel)
        }

        composable(Routes.SETTINGS) {
            SettingsScreen()
        }

        composable(
            route = Routes.DAY_DETAIL,
            arguments = listOf(
                navArgument("year") { type = NavType.IntType },
                navArgument("month") { type = NavType.IntType },
                navArgument("day") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: LocalDate.now().year
            val month = backStackEntry.arguments?.getInt("month") ?: LocalDate.now().monthValue
            val day = backStackEntry.arguments?.getInt("day") ?: LocalDate.now().dayOfMonth
            val date = LocalDate.of(year, month, day)
            val dayDetailViewModel: DayDetailViewModel = viewModel()

            DayDetailScreen(
                date = date,
                viewModel = dayDetailViewModel,
                onBack = {
                    calendarViewModel.loadData()
                    dashboardViewModel.loadData()
                    navController.popBackStack()
                },
                onOpenCamera = {
                    navController.navigate(Routes.photoEstimate(date, autoCamera = true))
                }
            )
        }

        composable(
            route = Routes.PHOTO_ESTIMATE,
            arguments = listOf(
                navArgument("year") { type = NavType.IntType },
                navArgument("month") { type = NavType.IntType },
                navArgument("day") { type = NavType.IntType },
                navArgument("autoCamera") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: LocalDate.now().year
            val month = backStackEntry.arguments?.getInt("month") ?: LocalDate.now().monthValue
            val day = backStackEntry.arguments?.getInt("day") ?: LocalDate.now().dayOfMonth
            val autoCamera = backStackEntry.arguments?.getBoolean("autoCamera") ?: false
            val date = LocalDate.of(year, month, day)
            val photoViewModel: PhotoEstimateViewModel = viewModel()

            PhotoEstimateScreen(
                date = date,
                viewModel = photoViewModel,
                autoLaunchCamera = autoCamera,
                onBack = { navController.popBackStack() },
                onSaved = {
                    calendarViewModel.loadData()
                    dashboardViewModel.loadData()
                    navController.popBackStack()
                }
            )
        }
    }
}

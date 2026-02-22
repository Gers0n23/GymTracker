package com.gcordero.gymtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.Text

import com.gcordero.gymtracker.ui.screens.dashboard.DashboardScreen
import com.gcordero.gymtracker.ui.screens.routines.RoutinesScreen
import com.gcordero.gymtracker.ui.screens.routines.RoutineDetailScreen
import com.gcordero.gymtracker.ui.screens.session.ActiveSessionScreen
import com.gcordero.gymtracker.ui.screens.metrics.BodyMetricsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

import androidx.compose.ui.Modifier

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) { Text("Login Screen") }
        composable(Screen.Register.route) { Text("Register Screen") }
        composable(Screen.Dashboard.route) { 
            DashboardScreen(navController = navController) 
        }
        composable(Screen.Routines.route) { 
            RoutinesScreen(
                onRoutineClick = { routineId -> 
                    navController.navigate(Screen.RoutineDetail.createRoute(routineId))
                }
            ) 
        }
        composable(
            route = Screen.RoutineDetail.route,
            arguments = listOf(navArgument("routineId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString("routineId") ?: ""
            RoutineDetailScreen(
                routineId = routineId,
                onBackClick = { navController.popBackStack() },
                onStartWorkout = { id -> 
                    navController.navigate(Screen.ActiveSession.createRoute(id))
                }
            )
        }
        composable(
            route = Screen.ActiveSession.route,
            arguments = listOf(navArgument("routineId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString("routineId") ?: ""
            ActiveSessionScreen(
                routineId = routineId,
                onFinish = { 
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.BodyMetrics.route) { BodyMetricsScreen() }
        composable(Screen.Progression.route) { Text("Progression Screen") }
    }
}

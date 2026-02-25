package com.gcordero.gymtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.Text
import androidx.compose.runtime.remember

import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.gcordero.gymtracker.ui.screens.dashboard.DashboardScreen
import com.gcordero.gymtracker.ui.screens.routines.RoutinesScreen
import com.gcordero.gymtracker.ui.screens.routines.RoutineDetailScreen
import com.gcordero.gymtracker.ui.screens.session.ActiveSessionScreen
import com.gcordero.gymtracker.ui.screens.session.SessionSummaryScreen
import com.gcordero.gymtracker.ui.screens.metrics.BodyMetricsScreen
import com.gcordero.gymtracker.ui.screens.history.WorkoutHistoryScreen
import com.gcordero.gymtracker.ui.screens.history.SessionDetailScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

import androidx.compose.ui.Modifier

// Simple in-memory holder to pass WorkoutSession object between screens without serialization
object SessionHolder {
    var selectedSession: WorkoutSession? = null
}

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
                    navController.navigate("session_summary") {
                        popUpTo(Screen.Dashboard.route)
                    }
                },
                onAbandon = {
                    navController.popBackStack()
                }
            )
        }
        composable("session_summary") {
            val session = SessionHolder.selectedSession
            if (session != null) {
                SessionSummaryScreen(
                    session = session,
                    onFinish = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            } else {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            }
        }
        composable(Screen.BodyMetrics.route) { BodyMetricsScreen() }
        composable(Screen.Progression.route) { Text("Progression Screen") }

        // Historial de entrenamientos
        composable(Screen.WorkoutHistory.route) {
            WorkoutHistoryScreen(
                onBackClick = { navController.popBackStack() },
                onSessionClick = { sessionId ->
                    navController.navigate(Screen.SessionDetail.createRoute(sessionId))
                }
            )
        }

        // Detalle / edición de sesión pasada
        composable(
            route = Screen.SessionDetail.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            val session = SessionHolder.selectedSession
            if (session != null) {
                SessionDetailScreen(
                    session = session,
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                // Fallback por si se llega sin sesión en el holder
                Text("Sesión no encontrada.", color = androidx.compose.ui.graphics.Color.Gray)
            }
        }
    }
}

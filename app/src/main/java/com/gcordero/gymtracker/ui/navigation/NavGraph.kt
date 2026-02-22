package com.gcordero.gymtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.Text

import com.gcordero.gymtracker.ui.screens.dashboard.DashboardScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Login.route) { Text("Login Screen") }
        composable(Screen.Register.route) { Text("Register Screen") }
        composable(Screen.Dashboard.route) { DashboardScreen() }
        composable(Screen.Routines.route) { Text("Routines Screen") }
        composable(Screen.BodyMetrics.route) { Text("Body Metrics Screen") }
        composable(Screen.Progression.route) { Text("Progression Screen") }
    }
}

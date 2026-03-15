package com.gcordero.gymtracker.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object Routines : Screen("routines")
    data object RoutineDetail : Screen("routine_detail/{routineId}") {
        fun createRoute(routineId: String) = "routine_detail/$routineId"
    }
    data object ActiveSession : Screen("active_session/{routineId}") {
        fun createRoute(routineId: String) = "active_session/$routineId"
    }
    data object BodyMetrics : Screen("body_metrics")
    data object Progression : Screen("progression")
    data object WorkoutHistory : Screen("workout_history")
    data object SessionDetail : Screen("session_detail/{sessionId}") {
        fun createRoute(sessionId: String) = "session_detail/$sessionId"
    }
    data object Nutrition : Screen("nutrition")
}

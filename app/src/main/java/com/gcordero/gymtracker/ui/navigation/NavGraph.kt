package com.gcordero.gymtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.Text
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth

import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.gcordero.gymtracker.ui.screens.auth.LoginScreen
import com.gcordero.gymtracker.ui.screens.auth.RegisterScreen
import com.gcordero.gymtracker.ui.screens.dashboard.DashboardScreen
import com.gcordero.gymtracker.ui.screens.routines.RoutinesScreen
import com.gcordero.gymtracker.ui.screens.routines.RoutineDetailScreen
import com.gcordero.gymtracker.ui.screens.session.ActiveSessionScreen
import com.gcordero.gymtracker.ui.screens.session.SessionSummaryScreen
import com.gcordero.gymtracker.ui.screens.metrics.BodyMetricsScreen
import com.gcordero.gymtracker.ui.screens.history.WorkoutHistoryScreen
import com.gcordero.gymtracker.ui.screens.history.SessionDetailScreen
import com.gcordero.gymtracker.ui.screens.onboarding.OnboardingScreen
import com.gcordero.gymtracker.ui.screens.nutrition.NutritionScreen

// Simple in-memory holder to pass WorkoutSession object between screens without serialization
object SessionHolder {
    var selectedSession: WorkoutSession? = null
}

private fun isProfileSet(context: android.content.Context, userId: String): Boolean =
    context.getSharedPreferences("body_prefs_$userId", android.content.Context.MODE_PRIVATE)
        .getBoolean("profile_set", false)

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // ── Auth state tracking ──────────────────────────────────────────────────
    // Track the current user UID (String comparison is safe, FirebaseUser equality is not)
    var currentUserId by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUserId = auth.currentUser?.uid
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }

    // React to auth state changes after initial composition
    LaunchedEffect(currentUserId) {
        val route = navController.currentDestination?.route ?: return@LaunchedEffect
        val authRoutes = setOf(Screen.Login.route, Screen.Register.route, Screen.Onboarding.route)

        if (currentUserId == null && route !in authRoutes) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        } else if (currentUserId != null && (route == Screen.Login.route || route == Screen.Register.route)) {
            val dest = if (isProfileSet(context, currentUserId!!)) Screen.Dashboard.route
                       else Screen.Onboarding.route
            navController.navigate(dest) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val startDestination = remember {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        when {
            uid == null                     -> Screen.Login.route
            isProfileSet(context, uid)      -> Screen.Dashboard.route
            else                            -> Screen.Onboarding.route
        }
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = modifier
    ) {
        // ── Auth ────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ── Onboarding ──────────────────────────────────────────────────────
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ── Main app ────────────────────────────────────────────────────────
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }
        composable(Screen.Routines.route) { backStackEntry ->
            RoutinesScreen(
                onRoutineClick = { routineId ->
                    if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        navController.navigate(Screen.RoutineDetail.createRoute(routineId))
                    }
                }
            )
        }
        composable(
            route     = Screen.RoutineDetail.route,
            arguments = listOf(navArgument("routineId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString("routineId") ?: ""
            RoutineDetailScreen(
                routineId    = routineId,
                onBackClick  = { navController.popBackStack() },
                onStartWorkout = { id ->
                    if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        navController.navigate(Screen.ActiveSession.createRoute(id))
                    }
                }
            )
        }
        composable(
            route     = Screen.ActiveSession.route,
            arguments = listOf(navArgument("routineId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString("routineId") ?: ""
            ActiveSessionScreen(
                routineId = routineId,
                onFinish  = {
                    if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        navController.navigate("session_summary") {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                },
                onAbandon = { navController.popBackStack() }
            )
        }
        composable("session_summary") {
            val session = SessionHolder.selectedSession
            if (session != null) {
                SessionSummaryScreen(
                    session  = session,
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
        composable(Screen.BodyMetrics.route)  { BodyMetricsScreen() }
        composable(Screen.Nutrition.route)    { NutritionScreen() }
        composable(Screen.Progression.route)  { Text("Progression Screen") }

        // ── Historial ───────────────────────────────────────────────────────
        composable(Screen.WorkoutHistory.route) { backStackEntry ->
            WorkoutHistoryScreen(
                onBackClick    = { navController.popBackStack() },
                onSessionClick = { sessionId ->
                    if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        navController.navigate(Screen.SessionDetail.createRoute(sessionId))
                    }
                }
            )
        }
        composable(
            route     = Screen.SessionDetail.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            val session = SessionHolder.selectedSession
            if (session != null) {
                SessionDetailScreen(
                    session     = session,
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                Text("Sesión no encontrada.", color = androidx.compose.ui.graphics.Color.Gray)
            }
        }
    }
}

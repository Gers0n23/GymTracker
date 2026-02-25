package com.gcordero.gymtracker.ui.screens.dashboard

// ── Muscle recovery ───────────────────────────────────────────────────────────
enum class MuscleStatus { TODAY, READY, RECOVERING, FATIGUED }

data class MuscleGroup(
    val id: String,
    val name: String,
    val status: MuscleStatus,
    val restInfo: String,
    val volumes: List<Int>      // kg·reps per week, last 4 weeks (oldest → newest)
)

// ── Smart tips ────────────────────────────────────────────────────────────────
enum class TipType { WEIGHT, REPS, SETS }

data class SmartTip(
    val exercise: String,
    val message: String,
    val type: TipType
)

// ── Hero card ─────────────────────────────────────────────────────────────────
data class TodayWorkout(
    val routineId: String,
    val dayTag: String,
    val routineName: String,
    val exercises: List<String>,
    val extraCount: Int,
    val estimatedMinutes: Int
)

// ── Dashboard UI state ────────────────────────────────────────────────────────
data class DashboardUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val greeting: String = "Hola",
    val streak: Int = 0,
    val sessionsThisMonth: Int = 0,
    val prsThisWeek: Int = 0,
    val activeDaysThisWeek: Int = 0,
    val activeDaysTarget: Int = 5,          // working days per week
    val weeklyBarHeights: List<Float> = List(7) { 0f },
    val todayIdx: Int = 0,
    val weeklyTotalKg: Double = 0.0,
    val weeklyChangePercent: Double? = null,
    val muscleGroups: Map<String, MuscleGroup> = emptyMap(),
    val todayWorkout: TodayWorkout? = null,
    val smartTips: List<SmartTip> = emptyList()
)

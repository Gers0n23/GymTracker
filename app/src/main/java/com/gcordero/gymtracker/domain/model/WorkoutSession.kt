package com.gcordero.gymtracker.domain.model

import com.google.firebase.Timestamp

data class WorkoutSession(
    val id: String = "",
    val userId: String = "",
    val routineId: String = "",
    val routineName: String = "",
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp? = null,
    val totalWeightLifted: Double = 0.0,
    val comments: String = "",
    val rpe: Int? = null, // Rate of Perceived Exertion
    val sleepQuality: Int? = null, // 1–5
    val energyLevel: Int? = null,   // 1–5
    val proteinConsumedPreviousDay: Int? = null, // Track previous day's complete protein intake
    val caloriesBurned: Int = 0 // Estimated kcal burned during the session
)

data class SetRecord(
    val id: String = "",
    val sessionId: String = "",
    val exerciseId: String = "",
    val exerciseName: String = "",
    val setNumber: Int = 1,
    // STRENGTH
    val weight: Double = 0.0,
    val reps: Int = 0,
    val rir: Int? = null, // Reps In Reserve
    // TIMED (plancha, isométricos)
    val durationSeconds: Int? = null,
    // CARDIO (trotadora, elíptica)
    val speedKmh: Double? = null,
    val inclinePercent: Double? = null,
    val isPersonalBest: Boolean = false,
    val timestamp: Timestamp = Timestamp.now()
) {
    /** Distancia estimada en km (solo para CARDIO). */
    val distanceKm: Double?
        get() = if (speedKmh != null && durationSeconds != null)
            speedKmh * (durationSeconds / 3600.0)
        else null
}

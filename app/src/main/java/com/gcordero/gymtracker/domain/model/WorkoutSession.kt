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
    val rpe: Int? = null // Rate of Perceived Exertion
)

data class SetRecord(
    val id: String = "",
    val sessionId: String = "",
    val exerciseId: String = "",
    val exerciseName: String = "",
    val setNumber: Int = 1,
    val weight: Double = 0.0,
    val reps: Int = 0,
    val rir: Int? = null, // Reps In Reserve
    val isPersonalBest: Boolean = false,
    val timestamp: Timestamp = Timestamp.now()
)

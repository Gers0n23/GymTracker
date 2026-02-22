package com.gcordero.gymtracker.domain.model

import com.google.firebase.Timestamp

data class BodyMetric(
    val id: String = "",
    val userId: String = "",
    val weightKg: Double = 0.0,
    val fatPercentage: Double? = null,
    val musclePercentage: Double? = null,
    val imc: Double? = null,
    val timestamp: Timestamp = Timestamp.now()
)

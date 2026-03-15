package com.gcordero.gymtracker.domain.model

import com.google.firebase.Timestamp

data class BodyMetric(
    val id: String = "",
    val userId: String = "",
    val weightKg: Double = 0.0,
    val fatPercentage: Double? = null,
    val musclePercentage: Double? = null,
    val imc: Double? = null,
    val timestamp: Timestamp = Timestamp.now(),

    // Medidas corporales (cm) — opcionales, tomadas con cinta métrica
    val neckCm: Double? = null,       // Cuello
    val chestCm: Double? = null,      // Pecho
    val waistCm: Double? = null,      // Cintura
    val hipCm: Double? = null,        // Cadera
    val bicepCm: Double? = null,      // Bíceps (brazo)
    val forearmCm: Double? = null,    // Antebrazo
    val thighCm: Double? = null,      // Muslo
    val calfCm: Double? = null        // Pantorrilla
)

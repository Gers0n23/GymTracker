package com.gcordero.gymtracker.domain.model

enum class ExerciseType {
    STRENGTH,  // peso + repeticiones (por defecto)
    TIMED,     // solo duración por serie (ej: plancha, isométricos)
    CARDIO     // velocidad + inclinación + duración total (ej: trotadora, elíptica)
}

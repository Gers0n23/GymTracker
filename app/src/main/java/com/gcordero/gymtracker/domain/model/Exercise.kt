package com.gcordero.gymtracker.domain.model

import com.google.firebase.firestore.Exclude

data class Exercise(
    val id: String = "",
    val routineId: String = "",
    // Referencia al ejercicio en el catálogo global (vacío si fue creado manualmente)
    val catalogExerciseId: String = "",
    val name: String = "",
    val muscleGroup: String = "",
    val equipment: String = "",
    val mediaUrl: String = "",
    val order: Int = 0,
    val notes: String = "",
    val targetSets: Int = 3,
    // Peso inicial sugerido para la primera sesión de este ejercicio en la rutina
    val initialWeight: Double = 0.0,
    // Almacenado como String para compatibilidad con Firestore.
    // Documentos existentes sin este campo usarán "STRENGTH" por defecto.
    val exerciseType: String = ExerciseType.STRENGTH.name
) {
    /** Acceso type-safe al tipo de ejercicio. */
    @get:Exclude
    val type: ExerciseType
        get() = ExerciseType.entries.find { it.name == exerciseType } ?: ExerciseType.STRENGTH
}

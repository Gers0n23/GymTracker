package com.gcordero.gymtracker.domain.model

data class Exercise(
    val id: String = "",
    val routineId: String = "",
    val name: String = "",
    val muscleGroup: String = "",
    val equipment: String = "",
    val mediaUrl: String = "",
    val order: Int = 0,
    val notes: String = "",
    val targetSets: Int = 3,
    // Almacenado como String para compatibilidad con Firestore.
    // Documentos existentes sin este campo usarán "STRENGTH" por defecto.
    val exerciseType: String = ExerciseType.STRENGTH.name
) {
    /** Acceso type-safe al tipo de ejercicio. */
    val type: ExerciseType
        get() = ExerciseType.entries.find { it.name == exerciseType } ?: ExerciseType.STRENGTH
}

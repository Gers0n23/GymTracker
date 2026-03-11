package com.gcordero.gymtracker.domain.model

/**
 * Representa un ejercicio del catálogo global compartido entre todos los usuarios.
 * Colección de Firestore: "exercise_catalog" (sin userId, es global).
 */
data class CatalogExercise(
    val id: String = "",
    val name: String = "",
    // Grupo muscular principal (ej: "Pecho", "Espalda", "Piernas", etc.)
    val muscleGroup: String = "",
    // Músculos secundarios trabajados
    val secondaryMuscles: List<String> = emptyList(),
    // Equipamiento requerido (ej: "Barra", "Mancuernas", "Polea alta")
    val equipment: String = "",
    // STRENGTH, TIMED o CARDIO
    val exerciseType: String = ExerciseType.STRENGTH.name,
    // URL de video demostrativo de referencia
    val mediaUrl: String = "",
    // Instrucciones / tips de técnica
    val notes: String = "",
    /**
     * Lista de sinónimos searchables: nombres en inglés, nombres de máquinas,
     * formas coloquiales, variantes, etc. Se usa para buscar de forma flexible.
     * Ejemplo para "Jalón al Pecho": ["Lat Pulldown", "Jalón Dorsal", "Polea al Pecho"]
     */
    val aliases: List<String> = emptyList()
) {
    val type: ExerciseType
        get() = ExerciseType.entries.find { it.name == exerciseType } ?: ExerciseType.STRENGTH
}

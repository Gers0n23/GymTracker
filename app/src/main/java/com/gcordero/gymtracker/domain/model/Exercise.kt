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
    val targetSets: Int = 3
)

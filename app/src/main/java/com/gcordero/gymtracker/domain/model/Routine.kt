package com.gcordero.gymtracker.domain.model

data class Routine(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val daysOfWeek: List<Int> = emptyList(), // 1 = Monday, 7 = Sunday
    val muscleGroups: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

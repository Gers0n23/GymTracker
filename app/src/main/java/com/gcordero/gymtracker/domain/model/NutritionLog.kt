package com.gcordero.gymtracker.domain.model

import com.google.firebase.Timestamp

data class NutritionLog(
    val id: String = "",
    val userId: String = "",
    val description: String = "",
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val mealType: String = MealType.SNACK.name,
    val dateKey: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

package com.gcordero.gymtracker.domain.model

enum class MealType(val label: String, val emoji: String) {
    BREAKFAST("Desayuno", "🌅"),
    LUNCH("Almuerzo", "☀️"),
    DINNER("Cena", "🌙"),
    SNACK("Snack", "🍎")
}

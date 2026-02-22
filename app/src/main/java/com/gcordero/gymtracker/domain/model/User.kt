package com.gcordero.gymtracker.domain.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val heightCm: Double? = null,
    val birthDate: Timestamp? = null,
    val weightUnit: String = "kg",
    val createdAt: Timestamp = Timestamp.now()
)

package com.gcordero.gymtracker.domain.model

data class ProgressInsight(
    val resumenGeneral: String,
    val categorias: List<InsightCategory>
)

data class InsightCategory(
    val titulo: String,
    val resumen: String,
    val acciones: List<InsightAction>
)

data class InsightAction(val texto: String)

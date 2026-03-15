package com.gcordero.gymtracker.ui.screens.metrics

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.DashboardRepository
import com.gcordero.gymtracker.data.repository.NutritionRepository
import com.gcordero.gymtracker.domain.model.BodyMetric
import com.gcordero.gymtracker.domain.model.InsightAction
import com.gcordero.gymtracker.domain.model.InsightCategory
import com.gcordero.gymtracker.domain.model.NutritionLog
import com.gcordero.gymtracker.domain.model.ProgressInsight
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ── Analysis window selector ──────────────────────────────────────────────────

enum class AnalysisWindow(val labelEs: String, val days: Int) {
    WEEKS_2("2 semanas", 14),
    MONTH_1("1 mes",     30),
    MONTHS_3("3 meses",  90)
}

// ── State ─────────────────────────────────────────────────────────────────────

sealed class InsightState {
    object Idle    : InsightState()
    object Loading : InsightState()
    data class Success(val insight: ProgressInsight) : InsightState()
    data class Error(val message: String)            : InsightState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ProgressInsightViewModel(application: Application) : AndroidViewModel(application) {

    private val nutritionRepo  = NutritionRepository()
    private val dashboardRepo  = DashboardRepository()
    private val auth           = FirebaseAuth.getInstance()
    private val db             = FirebaseFirestore.getInstance()
    private val userId get()   = auth.currentUser?.uid ?: "test_user"
    private val prefs get()    = getApplication<Application>()
        .getSharedPreferences("body_prefs_$userId", Context.MODE_PRIVATE)

    private val _insightState   = MutableStateFlow<InsightState>(InsightState.Idle)
    val insightState: StateFlow<InsightState> = _insightState.asStateFlow()

    private val _selectedWindow = MutableStateFlow(AnalysisWindow.MONTH_1)
    val selectedWindow: StateFlow<AnalysisWindow> = _selectedWindow.asStateFlow()

    private val analysisModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash")
    }

    init {
        viewModelScope.launch { runCatching { analysisModel } }
    }

    fun selectWindow(window: AnalysisWindow) {
        _selectedWindow.value = window
        val current = _insightState.value
        if (current is InsightState.Success || current is InsightState.Error) {
            _insightState.value = InsightState.Idle
        }
    }

    fun analyze() {
        if (_insightState.value is InsightState.Loading) return
        _insightState.value = InsightState.Loading

        viewModelScope.launch {
            runCatching {
                val window = _selectedWindow.value

                // ── Read profile from SharedPreferences ──────────────────────
                val currentWeight = prefs.getFloat("latest_weight_kg", 0f).toDouble()
                val heightCm      = prefs.getInt("height_cm", 0)
                val age           = prefs.getInt("age", 0)
                val isMale        = prefs.getBoolean("is_male", true)
                val goal          = prefs.getString("goal", "muscle") ?: "muscle"

                if (currentWeight <= 0.0) {
                    throw Exception("No hay datos de peso registrados. Agrega una métrica corporal primero.")
                }

                val targets = BodyMetricsViewModel.calculateMacros(
                    currentWeight, heightCm, age, isMale, goal
                )

                // ── Date range computation ────────────────────────────────────
                val dateFmt      = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                val calNow       = Calendar.getInstance()
                val endDateKey   = dateFmt.format(calNow.time)
                val endTimestamp = Timestamp(calNow.time)
                calNow.add(Calendar.DAY_OF_YEAR, -window.days)
                val startDateKey   = dateFmt.format(calNow.time)
                val sinceTimestamp = Timestamp(calNow.time)

                // ── Three parallel Firestore fetches ──────────────────────────
                val metricsDeferred   = async { fetchBodyMetricsForPeriod(userId, sinceTimestamp) }
                val nutritionDeferred = async { nutritionRepo.getLogsForPeriod(userId, startDateKey, endDateKey) }
                val sessionsDeferred  = async { dashboardRepo.getRecentSessions(userId, sinceTimestamp) }

                val bodyMetrics   = metricsDeferred.await()
                val nutritionLogs = nutritionDeferred.await()
                val sessions      = sessionsDeferred.await()

                if (bodyMetrics.isEmpty() && nutritionLogs.isEmpty() && sessions.isEmpty()) {
                    throw Exception("No hay suficientes datos en el periodo seleccionado.")
                }

                val setRecords = if (sessions.isNotEmpty()) {
                    dashboardRepo.getSetRecordsForSessions(sessions.map { it.id })
                } else emptyList()

                // ── Build summaries ───────────────────────────────────────────
                val latestMetric = bodyMetrics.firstOrNull()
                val imc          = latestMetric?.imc
                val imcStr       = if (imc != null && imc > 0) "%.1f".format(imc) else "Sin datos"
                val goalLabel    = when (goal) {
                    "muscle" -> "Ganancia muscular"
                    "cut"    -> "Pérdida de grasa"
                    else     -> "Mantenimiento"
                }
                val genderLabel = if (isMale) "Hombre" else "Mujer"

                val bodyText      = buildBodyMetricsSummary(bodyMetrics)
                val nutritionText = buildNutritionSummary(nutritionLogs, targets, window.days)
                val trainingText  = buildTrainingSummary(sessions, setRecords, currentWeight)

                val prompt = buildPrompt(
                    genderLabel, age, heightCm, currentWeight, goalLabel,
                    imcStr, targets, window.labelEs,
                    bodyText, nutritionText, trainingText
                )

                // ── Call Gemini ───────────────────────────────────────────────
                val response = analysisModel.generateContent(content { text(prompt) })
                val raw      = response.text?.trim()
                    ?: throw Exception("Respuesta vacía del modelo.")

                parseInsightResponse(raw)

            }.onSuccess { insight ->
                _insightState.value = InsightState.Success(insight)
            }.onFailure { e ->
                _insightState.value = InsightState.Error(
                    when {
                        e.message?.contains("No hay datos de peso") == true       -> e.message!!
                        e.message?.contains("No hay suficientes datos") == true   -> e.message!!
                        else -> "Error al analizar. Intenta de nuevo."
                    }
                )
            }
        }
    }

    // ── Firestore one-shot for body_metrics ──────────────────────────────────

    private suspend fun fetchBodyMetricsForPeriod(
        userId: String,
        since: Timestamp
    ): List<BodyMetric> {
        return try {
            db.collection("body_metrics")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", since)
                .get().await()
                .documents
                .mapNotNull { it.toObject(BodyMetric::class.java)?.copy(id = it.id) }
                .sortedByDescending { it.timestamp.seconds }
        } catch (e: Exception) {
            // Fallback: fetch all and filter client-side (avoids composite index requirement)
            try {
                db.collection("body_metrics")
                    .whereEqualTo("userId", userId)
                    .get().await()
                    .documents
                    .mapNotNull { it.toObject(BodyMetric::class.java)?.copy(id = it.id) }
                    .filter { it.timestamp.seconds >= since.seconds }
                    .sortedByDescending { it.timestamp.seconds }
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    // ── Data aggregation helpers ──────────────────────────────────────────────

    private fun buildBodyMetricsSummary(metrics: List<BodyMetric>): String {
        if (metrics.isEmpty()) return "Sin mediciones en el periodo."

        val latest = metrics.first()
        val oldest = metrics.last()
        val sb     = StringBuilder()

        sb.appendLine("Medición más reciente: peso=${latest.weightKg}kg" +
            (if (latest.fatPercentage != null) ", grasa=${latest.fatPercentage}%" else "") +
            (if (latest.musclePercentage != null) ", músculo=${latest.musclePercentage}%" else "") +
            (if (latest.imc != null && latest.imc > 0) ", IMC=${"%.1f".format(latest.imc)}" else ""))

        if (metrics.size > 1) {
            val weightDelta = latest.weightKg - oldest.weightKg
            val sign        = if (weightDelta >= 0) "+" else ""
            sb.appendLine("Cambio de peso en el periodo: ${sign}${"%.1f".format(weightDelta)} kg")

            val fatDelta = if (latest.fatPercentage != null && oldest.fatPercentage != null)
                latest.fatPercentage - oldest.fatPercentage else null
            if (fatDelta != null) {
                val fs = if (fatDelta >= 0) "+" else ""
                sb.appendLine("Cambio de grasa: ${fs}${"%.1f".format(fatDelta)}%")
            }

            val muscleDelta = if (latest.musclePercentage != null && oldest.musclePercentage != null)
                latest.musclePercentage - oldest.musclePercentage else null
            if (muscleDelta != null) {
                val ms = if (muscleDelta >= 0) "+" else ""
                sb.appendLine("Cambio de músculo: ${ms}${"%.1f".format(muscleDelta)}%")
            }

            sb.appendLine("Total de mediciones registradas: ${metrics.size}")
        } else {
            sb.appendLine("Solo hay una medición en el periodo (sin tendencia disponible).")
        }

        return sb.toString().trimEnd()
    }

    private fun buildNutritionSummary(
        logs: List<NutritionLog>,
        targets: MacroRecommendation,
        periodDays: Int
    ): String {
        if (logs.isEmpty()) return "Sin registros de alimentación en el periodo."

        val byDay      = logs.groupBy { it.dateKey }
        val daysLogged = byDay.size

        data class DailyTotals(val calories: Int, val protein: Double, val carbs: Double, val fat: Double)
        val dailyTotals = byDay.values.map { dayLogs ->
            DailyTotals(
                calories = dayLogs.sumOf { it.calories },
                protein  = dayLogs.sumOf { it.proteinG },
                carbs    = dayLogs.sumOf { it.carbsG },
                fat      = dayLogs.sumOf { it.fatG }
            )
        }

        val avgCalories = dailyTotals.map { it.calories }.average().toInt()
        val avgProtein  = dailyTotals.map { it.protein }.average()
        val avgCarbs    = dailyTotals.map { it.carbs }.average()
        val avgFat      = dailyTotals.map { it.fat }.average()

        val calDiff     = avgCalories - targets.calories
        val proteinDiff = avgProtein  - targets.proteinG

        return buildString {
            appendLine("Días con registro: $daysLogged / $periodDays")
            appendLine("Promedio diario: ${avgCalories} kcal | " +
                "${"%.0f".format(avgProtein)}g proteína | " +
                "${"%.0f".format(avgCarbs)}g carbos | " +
                "${"%.0f".format(avgFat)}g grasas")
            appendLine("Meta calórica: ${targets.calories} kcal/día — diferencia: " +
                "${if (calDiff >= 0) "+" else ""}${calDiff} kcal")
            appendLine("Meta proteína: ${targets.proteinG}g/día — diferencia: " +
                "${if (proteinDiff >= 0) "+" else ""}${"%.0f".format(proteinDiff)}g")
        }.trimEnd()
    }

    private fun buildTrainingSummary(
        sessions: List<WorkoutSession>,
        setRecords: List<SetRecord>,
        currentBodyWeight: Double
    ): String {
        if (sessions.isEmpty()) return "Sin sesiones de entrenamiento en el periodo."

        val strengthRecords = setRecords.filter { it.weight > 0 && it.reps > 0 }

        if (strengthRecords.isEmpty()) {
            return "Sesiones registradas: ${sessions.size}. Sin ejercicios de fuerza con datos de peso."
        }

        val sessionTimeMap = sessions.associate { it.id to it.startTime.seconds }

        val byExercise = strengthRecords.groupBy { it.exerciseName }
        val rankedExercises = byExercise.entries
            .sortedByDescending { it.value.map { r -> r.sessionId }.distinct().size }
            .take(8)

        val sb = StringBuilder()
        sb.appendLine("Sesiones completadas: ${sessions.size}")
        sb.appendLine("Ejercicios con seguimiento:")

        for ((exerciseName, records) in rankedExercises) {
            val sorted = records.sortedBy { sessionTimeMap[it.sessionId] ?: 0L }
            val first  = sorted.first()
            val last   = sorted.last()

            val weightDelta  = last.weight - first.weight
            val progressStr  = when {
                weightDelta > 0  -> "+${"%.1f".format(weightDelta)} kg"
                weightDelta < 0  -> "${"%.1f".format(weightDelta)} kg"
                else             -> "sin cambio"
            }
            val ratio    = if (currentBodyWeight > 0) last.weight / currentBodyWeight else 0.0
            val ratioStr = if (ratio > 0) " (${"%.2f".format(ratio)}x peso corporal)" else ""

            sb.appendLine("  - $exerciseName: " +
                "${"%.1f".format(first.weight)}kg×${first.reps}reps → " +
                "${"%.1f".format(last.weight)}kg×${last.reps}reps " +
                "(progresión: $progressStr)$ratioStr")
        }

        return sb.toString().trimEnd()
    }

    // ── Prompt builder ────────────────────────────────────────────────────────

    private fun buildPrompt(
        gender: String, age: Int, heightCm: Int, weight: Double,
        goalLabel: String, imcStr: String,
        targets: MacroRecommendation, periodLabel: String,
        bodyText: String, nutritionText: String, trainingText: String
    ): String = """
Eres un coach de fitness personal experto. Analiza el progreso del siguiente usuario y genera retroalimentación personalizada y específica.

PERFIL: $gender, $age años, ${heightCm}cm, ${weight}kg, Objetivo: $goalLabel
IMC actual: $imcStr

METAS CALCULADAS:
Calorías: ${targets.calories} kcal/día | Proteína: ${targets.proteinG}g | Carbs: ${targets.carbsG}g | Grasas: ${targets.fatG}g

MÉTRICAS CORPORALES (periodo: $periodLabel):
$bodyText

ALIMENTACIÓN (periodo: $periodLabel):
$nutritionText

ENTRENAMIENTO (ejercicios con progresión en el periodo):
$trainingText

TABLAS DE REFERENCIA (proporciones × peso corporal, hombres; mujeres ~60-65%):
Sentadilla:    Principiante 0.75x | Novato 1.0x | Intermedio 1.5x | Avanzado 1.75x | Elite 2.25x
Press Banca:   Principiante 0.5x  | Novato 0.75x | Intermedio 1.0x | Avanzado 1.25x | Elite 1.6x
Peso Muerto:   Principiante 1.0x  | Novato 1.25x | Intermedio 1.5x | Avanzado 2.0x  | Elite 2.5x
Press Militar: Principiante 0.35x | Novato 0.5x  | Intermedio 0.65x | Avanzado 1.0x | Elite 1.2x
Remo/Jalones:  Principiante 0.5x  | Novato 0.75x | Intermedio 1.0x | Avanzado 1.25x | Elite 1.5x

% GRASA CORPORAL (ACE): Hombres: Atlético 6-13% | Fitness 14-17% | Promedio 18-24% | Obeso >25%
                         Mujeres: Atlético 14-20% | Fitness 21-24% | Promedio 25-31% | Obeso >32%

GANANCIA MUSCULAR NATURAL (por mes): Principiante <1 año: 1-2kg | Intermedio 1-3 años: 0.5-1kg | Avanzado: 0.1-0.5kg
PROTEÍNA ÓPTIMA (ISSN): 1.6-2.2g/kg peso para músculo/fuerza

Responde SOLO con JSON, sin texto adicional ni bloques de código:
{"resumen_general":"<1-2 oraciones>","categorias":[{"titulo":"Cuerpo","resumen":"<análisis detallado>","acciones":[{"texto":"<acción concreta>"}]},{"titulo":"Alimentación","resumen":"...","acciones":[...]},{"titulo":"Entrenamiento","resumen":"...","acciones":[...]}]}
    """.trimIndent()

    // ── JSON parser ───────────────────────────────────────────────────────────

    private fun parseInsightResponse(raw: String): ProgressInsight {
        val json = raw
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        val root      = JSONObject(json)
        val resumen   = root.getString("resumen_general")
        val catsArray = root.getJSONArray("categorias")

        val categorias = (0 until catsArray.length()).map { i ->
            val cat      = catsArray.getJSONObject(i)
            val titulo   = cat.getString("titulo")
            val catRes   = cat.getString("resumen")
            val actArray = cat.getJSONArray("acciones")
            val acciones = (0 until actArray.length()).map { j ->
                InsightAction(texto = actArray.getJSONObject(j).getString("texto"))
            }
            InsightCategory(titulo = titulo, resumen = catRes, acciones = acciones)
        }

        return ProgressInsight(resumenGeneral = resumen, categorias = categorias)
    }
}

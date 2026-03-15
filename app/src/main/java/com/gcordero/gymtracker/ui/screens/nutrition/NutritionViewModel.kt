package com.gcordero.gymtracker.ui.screens.nutrition

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.NutritionRepository
import com.gcordero.gymtracker.data.repository.WorkoutRepository
import com.gcordero.gymtracker.domain.model.MealType
import com.gcordero.gymtracker.domain.model.NutritionLog
import com.gcordero.gymtracker.ui.screens.metrics.BodyMetricsViewModel
import com.gcordero.gymtracker.ui.screens.metrics.MacroRecommendation
import com.gcordero.gymtracker.ui.screens.metrics.ScannedMacros
import com.gcordero.gymtracker.ui.screens.metrics.ScanState
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = NutritionRepository()
    private val workoutRepo = WorkoutRepository()
    private val auth = FirebaseAuth.getInstance()
    private val userId get() = auth.currentUser?.uid ?: "test_user"

    private val prefs get() = getApplication<Application>()
        .getSharedPreferences("body_prefs_$userId", Context.MODE_PRIVATE)

    private val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    private val _selectedDate = MutableStateFlow(dateFormatter.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _logs = MutableStateFlow<List<NutritionLog>>(emptyList())
    val logs: StateFlow<List<NutritionLog>> = _logs.asStateFlow()

    private val _goals = MutableStateFlow(MacroRecommendation(160, 220, 70, 2100))
    val goals: StateFlow<MacroRecommendation> = _goals.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _caloriesBurnedToday = MutableStateFlow(0)
    val caloriesBurnedToday: StateFlow<Int> = _caloriesBurnedToday.asStateFlow()

    private val _pendingMealType = MutableStateFlow(MealType.SNACK)
    val pendingMealType: StateFlow<MealType> = _pendingMealType.asStateFlow()

    // Flash for images: better visual grounding and portion estimation
    private val visionModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash")
    }

    // Flash-lite for text: fast lookup, accuracy is equivalent
    private val textModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash-lite")
    }

    init {
        loadLogs()
        recalcGoals()
        loadCaloriesBurned()
        // Pre-warm both models to avoid cold start on first use
        viewModelScope.launch { runCatching { textModel } }
        viewModelScope.launch { runCatching { visionModel } }
    }

    private fun loadCaloriesBurned() {
        viewModelScope.launch {
            runCatching { _caloriesBurnedToday.value = workoutRepo.getTodayCaloriesBurned(userId) }
        }
    }

    private fun loadLogs() {
        viewModelScope.launch {
            repo.getTodayLogs(userId, _selectedDate.value).collect {
                _logs.value = it
            }
        }
    }

    private fun recalcGoals() {
        val weightKg = prefs.getFloat("latest_weight_kg", 75f).toDouble()
        val heightCm = prefs.getInt("height_cm", 175)
        val age = prefs.getInt("age", 25)
        val isMale = prefs.getBoolean("is_male", true)
        val goal = prefs.getString("goal", "muscle") ?: "muscle"
        _goals.value = BodyMetricsViewModel.calculateMacros(weightKg, heightCm, age, isMale, goal)
    }

    fun setPendingMealType(type: MealType) {
        _pendingMealType.value = type
    }

    fun scanFood(bitmap: Bitmap) {
        _scanState.value = ScanState.Loading
        viewModelScope.launch {
            runCatching {
                val prompt = """
                    Examina esta imagen y determina cuál de los siguientes casos aplica:

                    CASO A — La imagen contiene una tabla nutricional impresa (etiqueta de producto, nutrition facts, información nutricional):
                    - Tu tarea es OCR puro: transcribe los números exactamente como aparecen en la etiqueta. PROHIBIDO estimar o usar conocimiento externo.
                    - Si hay columna "por porción" / "1 porción" / "por porción (Xg)" y columna "por 100g", usa los valores de la columna de porción.
                    - Los decimales pueden estar con coma (ej: 20,1) — conviértelos a punto en el JSON (20.1).
                    - Si un valor no aparece en la tabla (ej: fibra), usa 0.

                    CASO B — La imagen muestra comida, plato o ingredientes sin etiqueta:
                    - Estima los macronutrientes visualmente basándote en el contenido visible.

                    CASO C — No hay comida ni tabla nutricional visible:
                    - Devuelve: {"error":"No se encontró comida en la imagen"}

                    Responde SOLO con JSON, sin texto adicional ni bloques de código:
                    {"descripcion":"<nombre del producto o plato>","calorias":<int>,"proteina_g":<float>,"carbohidratos_g":<float>,"grasas_g":<float>,"fibra_g":<float>}
                """.trimIndent()
                val response = visionModel.generateContent(content { image(bitmap); text(prompt) })
                parseGeminiResponse(response.text?.trim() ?: throw Exception("Respuesta vacía"))
            }.onSuccess { _scanState.value = ScanState.Success(it) }
             .onFailure { _scanState.value = ScanState.Error(it.message ?: "Error desconocido") }
        }
    }

    fun analyzeText(description: String) {
        _scanState.value = ScanState.Loading
        viewModelScope.launch {
            runCatching {
                val prompt = """
                    Estima los macronutrientes de: "$description"
                    Responde SOLO con JSON, sin texto adicional ni bloques de código:
                    {"descripcion":"<nombre>","calorias":<int>,"proteina_g":<float>,"carbohidratos_g":<float>,"grasas_g":<float>,"fibra_g":<float>}
                """.trimIndent()
                val response = textModel.generateContent(content { text(prompt) })
                parseGeminiResponse(response.text?.trim() ?: throw Exception("Respuesta vacía"))
            }.onSuccess { _scanState.value = ScanState.Success(it) }
             .onFailure { _scanState.value = ScanState.Error(it.message ?: "Error desconocido") }
        }
    }

    private fun parseGeminiResponse(raw: String): ScannedMacros {
        val json = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(json)
        if (obj.has("error")) throw Exception(obj.getString("error"))
        return ScannedMacros(
            description = obj.optString("descripcion", "Comida escaneada"),
            calories    = obj.optInt("calorias", 0),
            proteinG    = obj.optDouble("proteina_g", 0.0),
            carbsG      = obj.optDouble("carbohidratos_g", 0.0),
            fatG        = obj.optDouble("grasas_g", 0.0),
            fiberG      = obj.optDouble("fibra_g", 0.0)
        )
    }

    fun logNutrition(macros: ScannedMacros, mealType: MealType) {
        _scanState.value = ScanState.Idle  // cierra el diálogo inmediatamente
        viewModelScope.launch {
            repo.addLog(
                NutritionLog(
                    userId      = userId,
                    description = macros.description,
                    calories    = macros.calories,
                    proteinG    = macros.proteinG,
                    carbsG      = macros.carbsG,
                    fatG        = macros.fatG,
                    fiberG      = macros.fiberG,
                    mealType    = mealType.name,
                    timestamp   = Timestamp.now()
                ),
                dateKey = _selectedDate.value
            )
        }
    }

    fun updateLog(logId: String, macros: ScannedMacros, mealType: MealType) {
        viewModelScope.launch {
            repo.updateLog(
                logId,
                NutritionLog(
                    id          = logId,
                    userId      = userId,
                    description = macros.description,
                    calories    = macros.calories,
                    proteinG    = macros.proteinG,
                    carbsG      = macros.carbsG,
                    fatG        = macros.fatG,
                    fiberG      = macros.fiberG,
                    mealType    = mealType.name,
                    timestamp   = Timestamp.now()
                )
            )
        }
    }

    fun deleteLog(logId: String) {
        viewModelScope.launch { repo.deleteLog(logId) }
    }

    fun dismissScan() { _scanState.value = ScanState.Idle }
}

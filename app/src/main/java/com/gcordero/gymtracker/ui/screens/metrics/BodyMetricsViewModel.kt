package com.gcordero.gymtracker.ui.screens.metrics

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.BodyMetricsRepository
import com.gcordero.gymtracker.data.repository.NutritionRepository
import com.gcordero.gymtracker.domain.model.BodyMetric
import com.gcordero.gymtracker.domain.model.NutritionLog
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class MacroRecommendation(
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val calories: Int
)

data class ScannedMacros(
    val description: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double
)

sealed class ScanState {
    data object Idle : ScanState()
    data object Loading : ScanState()
    data class Success(val result: ScannedMacros) : ScanState()
    data class Error(val message: String) : ScanState()
}

class BodyMetricsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BodyMetricsRepository()
    private val nutritionRepo = NutritionRepository()
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid ?: "default"

    private val prefs = application.getSharedPreferences("body_prefs_$userId", Context.MODE_PRIVATE)
    private val todayKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

    // Gemini model via Firebase AI Logic (Google AI backend)
    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash")
    }

    private val _metrics = MutableStateFlow<List<BodyMetric>>(emptyList())
    val metrics: StateFlow<List<BodyMetric>> = _metrics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Profile ──────────────────────────────────────────────────────────────
    private val _heightCm = MutableStateFlow(prefs.getInt("height_cm", 0))
    val heightCm: StateFlow<Int> = _heightCm.asStateFlow()

    private val _age = MutableStateFlow(prefs.getInt("age", 0))
    val age: StateFlow<Int> = _age.asStateFlow()

    private val _isMale = MutableStateFlow(prefs.getBoolean("is_male", true))
    val isMale: StateFlow<Boolean> = _isMale.asStateFlow()

    private val _isProfileSet = MutableStateFlow(prefs.getBoolean("profile_set", false))
    val isProfileSet: StateFlow<Boolean> = _isProfileSet.asStateFlow()

    // ── Goal ─────────────────────────────────────────────────────────────────
    private val _goal = MutableStateFlow(prefs.getString("goal", "muscle") ?: "muscle")
    val goal: StateFlow<String> = _goal.asStateFlow()

    // ── Today's macros (SharedPreferences con clave de fecha) ─────────────────
    // Prefijo "m_" para evitar colisión con la clave "protein_" que era Int en versión anterior
    private val _todayProteinG = MutableStateFlow(prefs.getFloat("m_protein_$todayKey", 0f).toDouble())
    val todayProteinG: StateFlow<Double> = _todayProteinG.asStateFlow()

    private val _todayCarbsG = MutableStateFlow(prefs.getFloat("m_carbs_$todayKey", 0f).toDouble())
    val todayCarbsG: StateFlow<Double> = _todayCarbsG.asStateFlow()

    private val _todayFatG = MutableStateFlow(prefs.getFloat("m_fat_$todayKey", 0f).toDouble())
    val todayFatG: StateFlow<Double> = _todayFatG.asStateFlow()

    private val _todayFiberG = MutableStateFlow(prefs.getFloat("m_fiber_$todayKey", 0f).toDouble())
    val todayFiberG: StateFlow<Double> = _todayFiberG.asStateFlow()

    private val _todayCalories = MutableStateFlow(prefs.getInt("m_calories_$todayKey", 0))
    val todayCalories: StateFlow<Int> = _todayCalories.asStateFlow()

    // ── Scan state ────────────────────────────────────────────────────────────
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // ── Today's nutrition logs (Firestore) ────────────────────────────────────
    private val _todayLogs = MutableStateFlow<List<NutritionLog>>(emptyList())
    val todayLogs: StateFlow<List<NutritionLog>> = _todayLogs.asStateFlow()

    init {
        loadMetrics()
        loadTodayLogs()
    }

    private fun loadMetrics() {
        val uid = auth.currentUser?.uid ?: "test_user"
        viewModelScope.launch {
            _isLoading.value = true
            repository.getBodyMetrics(uid).collect {
                _metrics.value = it
                _isLoading.value = false
            }
        }
    }

    private fun loadTodayLogs() {
        val uid = auth.currentUser?.uid ?: "test_user"
        viewModelScope.launch {
            nutritionRepo.getTodayLogs(uid, todayKey).collect {
                _todayLogs.value = it
            }
        }
    }

    fun updateProfile(heightCm: Int, age: Int, isMale: Boolean) {
        _heightCm.value = heightCm
        _age.value = age
        _isMale.value = isMale
        _isProfileSet.value = true
        prefs.edit()
            .putInt("height_cm", heightCm)
            .putInt("age", age)
            .putBoolean("is_male", isMale)
            .putBoolean("profile_set", true)
            .apply()
    }

    fun setGoal(goal: String) {
        _goal.value = goal
        prefs.edit().putString("goal", goal).apply()
    }

    // ── Macro logging ─────────────────────────────────────────────────────────

    fun logNutrition(macros: ScannedMacros) {
        val uid = auth.currentUser?.uid ?: "test_user"
        // Update local state
        _todayProteinG.value  += macros.proteinG
        _todayCarbsG.value    += macros.carbsG
        _todayFatG.value      += macros.fatG
        _todayFiberG.value    += macros.fiberG
        _todayCalories.value  += macros.calories
        // Persist locally
        prefs.edit()
            .putFloat("m_protein_$todayKey",  _todayProteinG.value.toFloat())
            .putFloat("m_carbs_$todayKey",    _todayCarbsG.value.toFloat())
            .putFloat("m_fat_$todayKey",      _todayFatG.value.toFloat())
            .putFloat("m_fiber_$todayKey",    _todayFiberG.value.toFloat())
            .putInt("m_calories_$todayKey",   _todayCalories.value)
            .apply()
        // Persist in Firestore
        viewModelScope.launch {
            nutritionRepo.addLog(
                NutritionLog(
                    userId      = uid,
                    description = macros.description,
                    calories    = macros.calories,
                    proteinG    = macros.proteinG,
                    carbsG      = macros.carbsG,
                    fatG        = macros.fatG,
                    fiberG      = macros.fiberG
                ),
                dateKey = todayKey
            )
        }
        _scanState.value = ScanState.Idle
    }

    fun addMacrosManual(proteinG: Double, carbsG: Double, fatG: Double, calories: Int, fiberG: Double = 0.0) {
        logNutrition(ScannedMacros("Entrada manual", calories, proteinG, carbsG, fatG, fiberG))
    }

    fun resetTodayMacros() {
        _todayProteinG.value = 0.0
        _todayCarbsG.value   = 0.0
        _todayFatG.value     = 0.0
        _todayFiberG.value   = 0.0
        _todayCalories.value = 0
        prefs.edit()
            .putFloat("m_protein_$todayKey",  0f)
            .putFloat("m_carbs_$todayKey",    0f)
            .putFloat("m_fat_$todayKey",      0f)
            .putFloat("m_fiber_$todayKey",    0f)
            .putInt("m_calories_$todayKey",   0)
            .apply()
    }

    fun dismissScan() {
        _scanState.value = ScanState.Idle
    }

    // ── Gemini food scan ──────────────────────────────────────────────────────

    fun scanFood(bitmap: Bitmap) {
        _scanState.value = ScanState.Loading
        viewModelScope.launch {
            runCatching {
                val prompt = """
                    Analiza esta imagen de comida y estima los macronutrientes totales del plato completo.
                    Devuelve ÚNICAMENTE un JSON válido con este formato exacto, sin texto adicional ni bloques de código:
                    {
                      "descripcion": "nombre del plato",
                      "calorias": 450,
                      "proteina_g": 35.0,
                      "carbohidratos_g": 45.0,
                      "grasas_g": 12.0,
                      "fibra_g": 4.0
                    }
                    Si no puedes identificar comida en la imagen, devuelve:
                    {"error": "No se encontró comida en la imagen"}
                """.trimIndent()

                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                val raw = response.text?.trim() ?: throw Exception("Respuesta vacía del modelo")
                // Strip potential markdown code fences
                val json = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val obj  = JSONObject(json)

                if (obj.has("error")) throw Exception(obj.getString("error"))

                ScannedMacros(
                    description = obj.optString("descripcion", "Comida escaneada"),
                    calories    = obj.optInt("calorias", 0),
                    proteinG    = obj.optDouble("proteina_g", 0.0),
                    carbsG      = obj.optDouble("carbohidratos_g", 0.0),
                    fatG        = obj.optDouble("grasas_g", 0.0),
                    fiberG      = obj.optDouble("fibra_g", 0.0)
                )
            }.onSuccess { result ->
                _scanState.value = ScanState.Success(result)
            }.onFailure { e ->
                _scanState.value = ScanState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun addMetric(weight: Double, fat: Double?, muscle: Double?) {
        val uid    = auth.currentUser?.uid ?: "test_user"
        val heightM = if (_heightCm.value > 0) _heightCm.value / 100.0 else 1.75
        val imc     = weight / (heightM * heightM)
        viewModelScope.launch {
            repository.addBodyMetric(
                BodyMetric(
                    userId           = uid,
                    weightKg         = weight,
                    fatPercentage    = fat,
                    musclePercentage = muscle,
                    imc              = imc
                )
            )
        }
    }

    companion object {
        fun calculateMacros(
            weightKg: Double,
            heightCm: Int,
            age: Int,
            isMale: Boolean,
            goal: String
        ): MacroRecommendation {
            val h = if (heightCm > 0) heightCm else 175
            val a = if (age > 0) age else 25

            val bmr = if (isMale) {
                (10 * weightKg) + (6.25 * h) - (5 * a) + 5
            } else {
                (10 * weightKg) + (6.25 * h) - (5 * a) - 161
            }

            val tdee = bmr * 1.55
            val targetCal = when (goal) {
                "muscle" -> tdee + 300
                "cut"    -> tdee - 400
                else     -> tdee
            }

            val proteinG = when (goal) {
                "muscle" -> (weightKg * 2.0).roundToInt()
                "cut"    -> (weightKg * 2.2).roundToInt()
                else     -> (weightKg * 1.8).roundToInt()
            }.coerceAtLeast(50)

            val fatG      = ((targetCal * 0.25) / 9).roundToInt().coerceAtLeast(30)
            val remaining = targetCal - (proteinG * 4) - (fatG * 9)
            val carbsG    = (remaining / 4).roundToInt().coerceAtLeast(0)

            return MacroRecommendation(proteinG, carbsG, fatG, targetCal.roundToInt())
        }
    }
}

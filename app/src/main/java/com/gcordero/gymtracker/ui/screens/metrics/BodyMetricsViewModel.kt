package com.gcordero.gymtracker.ui.screens.metrics

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.BodyMetricsRepository
import com.gcordero.gymtracker.domain.model.BodyMetric
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.log10
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

// ── Body Analysis ─────────────────────────────────────────────────────────────

data class BodyAnalysis(
    /** % grasa US Navy (requiere cuello + cintura [+ cadera mujeres] + altura) */
    val navyFatPct: Double? = null,
    /** Relación Cintura-Cadera */
    val rcc: Double? = null,
    /** Relación Cintura-Estatura */
    val rce: Double? = null,
    /** Fat-Free Mass Index */
    val ffmi: Double? = null,
    /** Masa muscular libre de grasa (kg) */
    val leanMassKg: Double? = null,
    /** Ratio estético: pecho / cintura */
    val chestToWaist: Double? = null,
    /** Ratio estético: bíceps / cuello */
    val bicepToNeck: Double? = null,
    /** Ratio estético: muslo / pantorrilla */
    val thighToCalf: Double? = null,
    /** Clasificación de silueta */
    val bodyShape: BodyShape? = null
)

enum class BodyShape(val labelEs: String, val emoji: String) {
    V_SHAPE    ("V-Shape",       "🔺"),
    HOURGLASS  ("Reloj de arena","⏳"),
    PEAR       ("Pera",          "🍐"),
    APPLE      ("Manzana",       "🍎"),
    RECTANGLE  ("Rectángulo",    "📏")
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class BodyMetricsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BodyMetricsRepository()
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid ?: "default"

    private val prefs = application.getSharedPreferences("body_prefs_$userId", Context.MODE_PRIVATE)

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

    init {
        loadMetrics()
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

    fun addMetric(
        weight: Double, fat: Double?, muscle: Double?,
        neck: Double? = null, chest: Double? = null, waist: Double? = null,
        hip: Double? = null, bicep: Double? = null, forearm: Double? = null,
        thigh: Double? = null, calf: Double? = null
    ) {
        val uid     = auth.currentUser?.uid ?: "test_user"
        val heightM = if (_heightCm.value > 0) _heightCm.value / 100.0 else 1.75
        val imc     = weight / (heightM * heightM)
        prefs.edit().putFloat("latest_weight_kg", weight.toFloat()).apply()
        viewModelScope.launch {
            repository.addBodyMetric(
                BodyMetric(
                    userId           = uid,
                    weightKg         = weight,
                    fatPercentage    = fat,
                    musclePercentage = muscle,
                    imc              = imc,
                    neckCm           = neck,
                    chestCm          = chest,
                    waistCm          = waist,
                    hipCm            = hip,
                    bicepCm          = bicep,
                    forearmCm        = forearm,
                    thighCm          = thigh,
                    calfCm           = calf
                )
            )
        }
    }

    fun updateMetric(
        metric: BodyMetric, weight: Double, fat: Double?, muscle: Double?,
        neck: Double? = null, chest: Double? = null, waist: Double? = null,
        hip: Double? = null, bicep: Double? = null, forearm: Double? = null,
        thigh: Double? = null, calf: Double? = null
    ) {
        val heightM = if (_heightCm.value > 0) _heightCm.value / 100.0 else 1.75
        val imc     = weight / (heightM * heightM)
        val updated = metric.copy(
            weightKg         = weight,
            fatPercentage    = fat,
            musclePercentage = muscle,
            imc              = imc,
            neckCm           = neck,
            chestCm          = chest,
            waistCm          = waist,
            hipCm            = hip,
            bicepCm          = bicep,
            forearmCm        = forearm,
            thighCm          = thigh,
            calfCm           = calf
        )
        if (_metrics.value.firstOrNull()?.id == metric.id) {
            prefs.edit().putFloat("latest_weight_kg", weight.toFloat()).apply()
        }
        viewModelScope.launch { repository.updateBodyMetric(updated) }
    }

    fun deleteMetric(metricId: String) {
        viewModelScope.launch { repository.deleteBodyMetric(metricId) }
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

        /**
         * Derives all body analysis metrics from a [BodyMetric] record.
         * All results are null if the required measurements aren't available.
         */
        fun calculateBodyAnalysis(
            metric: BodyMetric,
            heightCm: Int,
            isMale: Boolean
        ): BodyAnalysis {
            val h = heightCm.toDouble().takeIf { it > 0 } ?: return BodyAnalysis()
            val hM = h / 100.0

            // ── 1. US Navy Body Fat % ─────────────────────────────────────
            val navyFat: Double? = run {
                val neck  = metric.neckCm ?: return@run null
                val waist = metric.waistCm ?: return@run null
                if (isMale) {
                    val diff = waist - neck
                    if (diff <= 0) return@run null
                    (86.010 * log10(diff) - 70.041 * log10(h) + 36.76)
                        .coerceIn(1.0, 60.0)
                } else {
                    val hip  = metric.hipCm ?: return@run null
                    val sum  = waist + hip - neck
                    if (sum <= 0) return@run null
                    (163.205 * log10(sum) - 97.684 * log10(h) - 78.387)
                        .coerceIn(1.0, 60.0)
                }
            }

            // ── 2. Relación Cintura-Cadera (RCC) ─────────────────────────
            val rcc: Double? = run {
                val waist = metric.waistCm ?: return@run null
                val hip   = metric.hipCm   ?: return@run null
                waist / hip
            }

            // ── 3. Relación Cintura-Estatura (RCE) ───────────────────────
            val rce: Double? = metric.waistCm?.let { it / h }

            // ── 4. FFMI + Lean Mass ───────────────────────────────────────
            // Use navyFat if available, else manual fat%
            val fatPctForCalc = navyFat ?: metric.fatPercentage
            val (ffmi, leanMass) = if (fatPctForCalc != null) {
                val lean = metric.weightKg * (1.0 - fatPctForCalc / 100.0)
                val f    = lean / (hM * hM)
                // Normalized FFMI accounts for height differences
                val fNorm = f + 6.1 * (1.8 - hM)
                Pair(fNorm, lean)
            } else Pair(null, null)

            // ── 5. Proporciones estéticas ─────────────────────────────────
            val chestToWaist: Double? = run {
                val chest = metric.chestCm ?: return@run null
                val waist = metric.waistCm ?: return@run null
                chest / waist
            }
            val bicepToNeck: Double? = run {
                val bicep = metric.bicepCm ?: return@run null
                val neck  = metric.neckCm  ?: return@run null
                bicep / neck
            }
            val thighToCalf: Double? = run {
                val thigh = metric.thighCm ?: return@run null
                val calf  = metric.calfCm  ?: return@run null
                thigh / calf
            }

            // ── 6. Clasificación de silueta ───────────────────────────────
            val bodyShape: BodyShape? = run {
                val chest = metric.chestCm ?: return@run null
                val waist = metric.waistCm ?: return@run null
                val hip   = metric.hipCm   ?: return@run null
                val chestVsHip    = (chest - hip) / hip          // positive → chest bigger
                val waistVsHip    = waist / hip
                val waistVsChest  = waist / chest
                when {
                    // Reloj de arena: pecho ≈ cadera, cintura << ambos
                    !isMale && kotlin.math.abs(chestVsHip) < 0.05 && waistVsHip < 0.75 ->
                        BodyShape.HOURGLASS
                    // V-shape: pecho bastante > cadera, cintura estrecha
                    chestVsHip > 0.10 && waistVsChest < 0.82 ->
                        BodyShape.V_SHAPE
                    // Pera: cadera bastante > pecho
                    chestVsHip < -0.10 ->
                        BodyShape.PEAR
                    // Manzana: cintura ancha relativa a cadera
                    waistVsHip > 0.93 ->
                        BodyShape.APPLE
                    else ->
                        BodyShape.RECTANGLE
                }
            }

            return BodyAnalysis(
                navyFatPct   = navyFat,
                rcc          = rcc,
                rce          = rce,
                ffmi         = ffmi,
                leanMassKg   = leanMass,
                chestToWaist = chestToWaist,
                bicepToNeck  = bicepToNeck,
                thighToCalf  = thighToCalf,
                bodyShape    = bodyShape
            )
        }
    }
}

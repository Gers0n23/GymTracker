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

class BodyMetricsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BodyMetricsRepository()
    private val auth   = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid ?: "default"

    // Preferencias separadas por usuario para que altura, edad, sexo y proteínas no se mezclen
    private val prefs = application.getSharedPreferences("body_prefs_$userId", Context.MODE_PRIVATE)
    private val todayKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

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

    // ── Goal: "muscle" | "maintain" | "cut" ──────────────────────────────────
    private val _goal = MutableStateFlow(prefs.getString("goal", "muscle") ?: "muscle")
    val goal: StateFlow<String> = _goal.asStateFlow()

    // ── Today's protein tracker ───────────────────────────────────────────────
    private val _todayProteinG = MutableStateFlow(prefs.getInt("protein_$todayKey", 0))
    val todayProteinG: StateFlow<Int> = _todayProteinG.asStateFlow()

    init { loadMetrics() }

    private fun loadMetrics() {
        val userId = auth.currentUser?.uid ?: "test_user"
        viewModelScope.launch {
            _isLoading.value = true
            repository.getBodyMetrics(userId).collect {
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

    fun addProtein(grams: Int) {
        val new = (_todayProteinG.value + grams).coerceAtLeast(0)
        _todayProteinG.value = new
        prefs.edit().putInt("protein_$todayKey", new).apply()
    }

    fun resetProtein() {
        _todayProteinG.value = 0
        prefs.edit().putInt("protein_$todayKey", 0).apply()
    }

    fun addMetric(weight: Double, fat: Double?, muscle: Double?) {
        val userId = auth.currentUser?.uid ?: "test_user"
        val heightM = if (_heightCm.value > 0) _heightCm.value / 100.0 else 1.75
        val imc = weight / (heightM * heightM)
        viewModelScope.launch {
            repository.addBodyMetric(
                BodyMetric(
                    userId = userId,
                    weightKg = weight,
                    fatPercentage = fat,
                    musclePercentage = muscle,
                    imc = imc
                )
            )
        }
    }

    companion object {
        /**
         * Calcula macros con Mifflin-St Jeor + ajuste por objetivo.
         * Actividad moderada (×1.55) como base para usuarios de gimnasio.
         */
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
                "muscle" -> (weightKg * 1.8).roundToInt()
                "cut"    -> (weightKg * 2.2).roundToInt()
                else     -> (weightKg * 1.6).roundToInt()
            }.coerceAtLeast(50)

            val fatG = ((targetCal * 0.25) / 9).roundToInt().coerceAtLeast(30)
            val remaining = targetCal - (proteinG * 4) - (fatG * 9)
            val carbsG = (remaining / 4).roundToInt().coerceAtLeast(0)

            return MacroRecommendation(proteinG, carbsG, fatG, targetCal.roundToInt())
        }
    }
}

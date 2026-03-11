package com.gcordero.gymtracker.ui.screens.onboarding

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.ExerciseCatalogRepository
import com.gcordero.gymtracker.data.util.DataPopulator
import com.gcordero.gymtracker.data.util.ExerciseCatalogSeeder
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.ExerciseType
import com.gcordero.gymtracker.domain.model.Routine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class OnboardingPlan { DAYS_5, PPL, FULL_BODY, NONE }

data class OnboardingUiState(
    val isMale: Boolean      = true,
    val age: String          = "",
    val heightCm: String     = "",
    val goalIndex: Int       = 0,
    val selectedPlan: OnboardingPlan = OnboardingPlan.DAYS_5,
    val isLoading: Boolean   = false,
    val isDone: Boolean      = false,
    val error: String?       = null
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun setIsMale(value: Boolean)              { _uiState.update { it.copy(isMale = value) } }
    fun setAge(value: String)                  { _uiState.update { it.copy(age = value) } }
    fun setHeightCm(value: String)             { _uiState.update { it.copy(heightCm = value) } }
    fun setGoalIndex(value: Int)               { _uiState.update { it.copy(goalIndex = value) } }
    fun setSelectedPlan(plan: OnboardingPlan)  { _uiState.update { it.copy(selectedPlan = plan) } }

    fun finish() {
        val userId = auth.currentUser?.uid ?: return
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val goalLabels = listOf("Hipertrofia", "Fuerza", "Definición", "Salud general")
                val prefs = getApplication<Application>()
                    .getSharedPreferences("body_prefs_$userId", Context.MODE_PRIVATE)
                    .edit()
                prefs.putBoolean("profile_set", true)
                prefs.putBoolean("is_male", s.isMale)
                s.age.toIntOrNull()?.let { prefs.putInt("age", it) }
                s.heightCm.toIntOrNull()?.let { prefs.putInt("height_cm", it) }
                prefs.putString("goal", goalLabels[s.goalIndex])
                prefs.apply()

                when (s.selectedPlan) {
                    OnboardingPlan.DAYS_5    -> DataPopulator.populateInitialData(userId)
                    OnboardingPlan.PPL       -> createPPLRoutines(userId)
                    OnboardingPlan.FULL_BODY -> createFullBodyRoutine(userId)
                    OnboardingPlan.NONE      -> Unit
                }
                // Pobla el catálogo global de ejercicios si está vacío (solo se ejecuta una vez, es global)
                ExerciseCatalogSeeder.seedIfEmpty(ExerciseCatalogRepository())
                _uiState.update { it.copy(isLoading = false, isDone = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }

    // ── PPL ──────────────────────────────────────────────────────────────────

    private suspend fun createPPLRoutines(userId: String) {
        listOf(pplPush(), pplPull(), pplLegs()).forEach { (routineBase, exercises) ->
            val ref = db.collection("routines").document()
            ref.set(routineBase.copy(id = ref.id, userId = userId)).await()
            exercises.forEachIndexed { i, ex ->
                val exRef = db.collection("exercises").document()
                exRef.set(ex.copy(id = exRef.id, routineId = ref.id, order = i)).await()
            }
        }
    }

    private fun pplPush() = Pair(
        Routine(
            name         = "Push – Pecho, Hombros, Tríceps",
            description  = "Empujes horizontales y verticales.",
            daysOfWeek   = listOf(1),
            muscleGroups = listOf("Pecho", "Hombros", "Tríceps")
        ),
        listOf(
            Exercise(name = "Chest Press en Máquina",  muscleGroup = "Pecho",   equipment = "Máquina de pecho",   targetSets = 4),
            Exercise(name = "Pec Deck / Aperturas",    muscleGroup = "Pecho",   equipment = "Pec Deck",           targetSets = 3),
            Exercise(name = "Press de Hombros",        muscleGroup = "Hombros", equipment = "Máquina de hombros", targetSets = 3),
            Exercise(name = "Elevaciones Laterales",   muscleGroup = "Hombros", equipment = "Máquina lateral",    targetSets = 3),
            Exercise(name = "Extensión de Tríceps",    muscleGroup = "Tríceps", equipment = "Polea alta",         targetSets = 3),
            Exercise(name = "Fondos Asistidos",        muscleGroup = "Tríceps", equipment = "Máquina de fondos",  targetSets = 2)
        )
    )

    private fun pplPull() = Pair(
        Routine(
            name         = "Pull – Espalda, Bíceps",
            description  = "Tracciones y curl.",
            daysOfWeek   = listOf(3),
            muscleGroups = listOf("Espalda", "Bíceps")
        ),
        listOf(
            Exercise(name = "Jalón al Pecho",          muscleGroup = "Espalda", equipment = "Polea alta",       targetSets = 4),
            Exercise(name = "Remo Sentado en Máquina", muscleGroup = "Espalda", equipment = "Polea baja",       targetSets = 3),
            Exercise(name = "Jalón Agarre Estrecho",   muscleGroup = "Espalda", equipment = "Polea alta",       targetSets = 3),
            Exercise(name = "Curl Bíceps en Máquina",  muscleGroup = "Bíceps",  equipment = "Máquina de curl",  targetSets = 3),
            Exercise(name = "Curl Polea Baja",         muscleGroup = "Bíceps",  equipment = "Polea baja",       targetSets = 2),
            Exercise(name = "Curl Martillo",           muscleGroup = "Bíceps",  equipment = "Mancuernas",       targetSets = 2)
        )
    )

    private fun pplLegs() = Pair(
        Routine(
            name         = "Legs – Piernas completas",
            description  = "Cuádriceps, femorales y glúteos.",
            daysOfWeek   = listOf(5),
            muscleGroups = listOf("Cuádriceps", "Isquiotibiales", "Glúteos")
        ),
        listOf(
            Exercise(name = "Prensa de Piernas",       muscleGroup = "Cuádriceps",     equipment = "Prensa de piernas",       targetSets = 4),
            Exercise(name = "Extensión de Cuádriceps", muscleGroup = "Cuádriceps",     equipment = "Máquina de extensión",    targetSets = 3),
            Exercise(name = "Curl Femoral",            muscleGroup = "Isquiotibiales", equipment = "Máquina de curl femoral", targetSets = 3),
            Exercise(name = "Hip Thrust en Máquina",   muscleGroup = "Glúteos",        equipment = "Máquina hip thrust",      targetSets = 3),
            Exercise(name = "Abductores en Máquina",   muscleGroup = "Abductores",     equipment = "Máquina abductores",      targetSets = 3),
            Exercise(name = "Gemelos en Máquina",      muscleGroup = "Gemelos",        equipment = "Máquina de gemelos",      targetSets = 3)
        )
    )

    // ── Full Body ─────────────────────────────────────────────────────────────

    private suspend fun createFullBodyRoutine(userId: String) {
        val ref = db.collection("routines").document()
        ref.set(
            Routine(
                id           = ref.id,
                userId       = userId,
                name         = "Full Body",
                description  = "Cuerpo completo, 3 días a la semana.",
                daysOfWeek   = listOf(1, 3, 5),
                muscleGroups = listOf("Cuádriceps", "Glúteos", "Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps", "Core")
            )
        ).await()

        listOf(
            Exercise(name = "Prensa de Piernas",       muscleGroup = "Cuádriceps", equipment = "Prensa de piernas",    targetSets = 3),
            Exercise(name = "Hip Thrust en Máquina",   muscleGroup = "Glúteos",    equipment = "Máquina hip thrust",   targetSets = 3),
            Exercise(name = "Chest Press en Máquina",  muscleGroup = "Pecho",      equipment = "Máquina de pecho",     targetSets = 3),
            Exercise(name = "Jalón al Pecho",          muscleGroup = "Espalda",    equipment = "Polea alta",           targetSets = 3),
            Exercise(name = "Press de Hombros",        muscleGroup = "Hombros",    equipment = "Máquina de hombros",   targetSets = 3),
            Exercise(name = "Curl Bíceps en Máquina",  muscleGroup = "Bíceps",     equipment = "Máquina de curl",      targetSets = 2),
            Exercise(name = "Extensión de Tríceps",    muscleGroup = "Tríceps",    equipment = "Polea alta",           targetSets = 2),
            Exercise(name = "Plancha Frontal",         muscleGroup = "Core",       equipment = "Suelo",                targetSets = 3,
                     exerciseType = ExerciseType.TIMED.name)
        ).forEachIndexed { i, ex ->
            val exRef = db.collection("exercises").document()
            exRef.set(ex.copy(id = exRef.id, routineId = ref.id, order = i)).await()
        }
    }
}

package com.gcordero.gymtracker.ui.screens.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.ExerciseCatalogRepository
import com.gcordero.gymtracker.data.repository.ExerciseRepository
import com.gcordero.gymtracker.data.repository.RoutineRepository
import com.gcordero.gymtracker.data.util.RoutineExportUtil
import com.gcordero.gymtracker.domain.model.CatalogExercise
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.Routine
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RoutineAnalysisState {
    object Idle : RoutineAnalysisState()
    object Loading : RoutineAnalysisState()
    data class Result(val text: String) : RoutineAnalysisState()
    data class Error(val message: String) : RoutineAnalysisState()
}

class RoutineDetailViewModel(
    private val routineRepository: RoutineRepository = RoutineRepository(),
    private val exerciseRepository: ExerciseRepository = ExerciseRepository(),
    private val catalogRepository: ExerciseCatalogRepository = ExerciseCatalogRepository()
) : ViewModel() {

    private val _routine = MutableStateFlow<Routine?>(null)
    val routine: StateFlow<Routine?> = _routine.asStateFlow()

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Catálogo global de ejercicios
    private val _catalogExercises = MutableStateFlow<List<CatalogExercise>>(emptyList())
    val catalogExercises: StateFlow<List<CatalogExercise>> = _catalogExercises.asStateFlow()

    private val _catalogMuscleGroups = MutableStateFlow<List<String>>(emptyList())
    val catalogMuscleGroups: StateFlow<List<String>> = _catalogMuscleGroups.asStateFlow()

    private val _isCatalogLoading = MutableStateFlow(false)
    val isCatalogLoading: StateFlow<Boolean> = _isCatalogLoading.asStateFlow()

    private val _analysisState = MutableStateFlow<RoutineAnalysisState>(RoutineAnalysisState.Idle)
    val analysisState: StateFlow<RoutineAnalysisState> = _analysisState.asStateFlow()

    private val geminiModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash-lite")
    }

    fun loadRoutineDetail(routineId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            // Fetch routine object
            val routineObj = routineRepository.getRoutineById(routineId)
            _routine.value = routineObj

            exerciseRepository.getExercisesByRoutine(routineId).collect {
                _exercises.value = it
                _isLoading.value = false
            }
        }
        // Carga el catálogo global en paralelo
        viewModelScope.launch {
            _isCatalogLoading.value = true
            val all = catalogRepository.getAllExercises()
            _catalogExercises.value = all
            _catalogMuscleGroups.value = all.map { it.muscleGroup }.distinct().sorted()
            _isCatalogLoading.value = false
        }
    }

    fun getExportText(): String? {
        val routine = _routine.value ?: return null
        val exercises = _exercises.value
        return RoutineExportUtil.serialize(routine, exercises)
    }

    fun analyzeRoutine() {
        val routine = _routine.value ?: return
        val exercises = _exercises.value
        if (exercises.isEmpty()) {
            _analysisState.value = RoutineAnalysisState.Error("Añade ejercicios a la rutina antes de analizarla.")
            return
        }
        _analysisState.value = RoutineAnalysisState.Loading
        viewModelScope.launch {
            runCatching {
                val json = RoutineExportUtil.toAnalysisJson(routine, exercises)
                val prompt = """
                    Eres un experto en fitness y ciencias del ejercicio. Analiza esta rutina de entrenamiento y proporciona en español:

                    1. **Evaluación general** (2-3 oraciones): balance, distribución muscular, puntos fuertes y débiles.
                    2. **Posibles problemas**: sobreentrenamiento, músculos descuidados, secuencia subóptima.
                    3. **Sugerencias concretas** (numeradas): cambios específicos con justificación breve.
                    4. **Calificación**: X/10 con una frase de resumen.

                    Sé directo, práctico y específico. Usa viñetas donde corresponda.

                    RUTINA:
                    $json
                """.trimIndent()
                val response = geminiModel.generateContent(content { text(prompt) })
                response.text?.trim() ?: throw Exception("Respuesta vacía de Gemini")
            }.onSuccess { _analysisState.value = RoutineAnalysisState.Result(it) }
             .onFailure { _analysisState.value = RoutineAnalysisState.Error(it.message ?: "Error desconocido") }
        }
    }

    fun dismissAnalysis() {
        _analysisState.value = RoutineAnalysisState.Idle
    }

    fun updateRoutine(routineId: String, name: String, description: String) {
        val currentRoutine = _routine.value ?: return
        val updatedRoutine = currentRoutine.copy(name = name, description = description)
        viewModelScope.launch {
            routineRepository.updateRoutine(updatedRoutine)
            _routine.value = updatedRoutine
        }
    }

    fun deleteRoutine(routineId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            routineRepository.deleteRoutine(routineId)
            onSuccess()
        }
    }

    fun addExercise(
        routineId: String,
        name: String,
        muscleGroup: String,
        mediaUrl: String = "",
        targetSets: Int = 3,
        exerciseType: String = "STRENGTH",
        catalogExerciseId: String = "",
        initialWeight: Double = 0.0,
        equipment: String = "",
        notes: String = ""
    ) {
        val newExercise = Exercise(
            routineId = routineId,
            catalogExerciseId = catalogExerciseId,
            name = name,
            muscleGroup = muscleGroup,
            equipment = equipment,
            notes = notes,
            mediaUrl = mediaUrl,
            order = _exercises.value.size,
            targetSets = targetSets,
            initialWeight = initialWeight,
            exerciseType = exerciseType
        )
        viewModelScope.launch {
            exerciseRepository.addExercise(newExercise)
        }
    }

    fun addExerciseFromCatalog(
        routineId: String,
        catalogExercise: CatalogExercise,
        targetSets: Int,
        initialWeight: Double
    ) {
        addExercise(
            routineId = routineId,
            name = catalogExercise.name,
            muscleGroup = catalogExercise.muscleGroup,
            mediaUrl = catalogExercise.mediaUrl,
            targetSets = targetSets,
            exerciseType = catalogExercise.exerciseType,
            catalogExerciseId = catalogExercise.id,
            initialWeight = initialWeight,
            equipment = catalogExercise.equipment,
            notes = catalogExercise.notes
        )
    }

    fun deleteExercise(exerciseId: String) {
        viewModelScope.launch {
            exerciseRepository.deleteExercise(exerciseId)
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            exerciseRepository.updateExercise(exercise)
        }
    }
}

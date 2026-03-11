package com.gcordero.gymtracker.ui.screens.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.ExerciseCatalogRepository
import com.gcordero.gymtracker.data.repository.ExerciseRepository
import com.gcordero.gymtracker.data.repository.RoutineRepository
import com.gcordero.gymtracker.domain.model.CatalogExercise
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.Routine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun loadRoutineDetail(routineId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Fecth routine object
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

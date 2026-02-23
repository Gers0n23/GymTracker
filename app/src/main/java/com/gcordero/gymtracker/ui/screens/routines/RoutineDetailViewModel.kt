package com.gcordero.gymtracker.ui.screens.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.ExerciseRepository
import com.gcordero.gymtracker.data.repository.RoutineRepository
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.Routine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RoutineDetailViewModel(
    private val routineRepository: RoutineRepository = RoutineRepository(),
    private val exerciseRepository: ExerciseRepository = ExerciseRepository()
) : ViewModel() {

    private val _routine = MutableStateFlow<Routine?>(null)
    val routine: StateFlow<Routine?> = _routine.asStateFlow()

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    fun addExercise(routineId: String, name: String, muscleGroup: String, mediaUrl: String = "") {
        val newExercise = Exercise(
            routineId = routineId,
            name = name,
            muscleGroup = muscleGroup,
            mediaUrl = mediaUrl,
            order = _exercises.value.size
        )
        viewModelScope.launch {
            exerciseRepository.addExercise(newExercise)
        }
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

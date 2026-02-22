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
            // In a real app we would fetch the routine object too
            // For now let's assume we have it or fetch it once
            // Actually, we can just fetch exercises
            exerciseRepository.getExercisesByRoutine(routineId).collect {
                _exercises.value = it
                _isLoading.value = false
            }
        }
    }

    fun addExercise(routineId: String, name: String, muscleGroup: String) {
        val newExercise = Exercise(
            routineId = routineId,
            name = name,
            muscleGroup = muscleGroup,
            order = _exercises.value.size
        )
        viewModelScope.launch {
            exerciseRepository.addExercise(newExercise)
        }
    }
}

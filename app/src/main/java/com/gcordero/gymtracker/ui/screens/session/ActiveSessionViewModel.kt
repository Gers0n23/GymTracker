package com.gcordero.gymtracker.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.ExerciseRepository
import com.gcordero.gymtracker.data.repository.WorkoutRepository
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActiveSessionViewModel(
    private val exerciseRepository: ExerciseRepository = ExerciseRepository(),
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _sets = MutableStateFlow<Map<String, List<SetRecord>>>(emptyMap())
    val sets: StateFlow<Map<String, List<SetRecord>>> = _sets.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow<Int?>(null)
    val restTimerSeconds: StateFlow<Int?> = _restTimerSeconds.asStateFlow()

    private var timerJob: Job? = null
    private var restTimerJob: Job? = null
    private val startTime = Timestamp.now()

    fun startSession(routineId: String) {
        viewModelScope.launch {
            exerciseRepository.getExercisesByRoutine(routineId).collect {
                _exercises.value = it
                // Initialize sets for each exercise if empty
                val initialSets = mutableMapOf<String, List<SetRecord>>()
                it.forEach { exercise ->
                    initialSets[exercise.id] = listOf(SetRecord(exerciseId = exercise.id, exerciseName = exercise.name))
                }
                _sets.value = initialSets
            }
        }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timerSeconds.value++
            }
        }
    }

    fun startRestTimer() {
        restTimerJob?.cancel()
        _restTimerSeconds.value = 120 // 2 minutes
        restTimerJob = viewModelScope.launch {
            while (_restTimerSeconds.value!! > 0) {
                delay(1000)
                _restTimerSeconds.value = _restTimerSeconds.value!! - 1
            }
            _restTimerSeconds.value = null
        }
    }

    fun cancelRestTimer() {
        restTimerJob?.cancel()
        _restTimerSeconds.value = null
    }

    fun addSet(exerciseId: String, exerciseName: String) {
        val currentSets = _sets.value.toMutableMap()
        val exerciseSets = currentSets[exerciseId]?.toMutableList() ?: mutableListOf()
        exerciseSets.add(SetRecord(
            exerciseId = exerciseId, 
            exerciseName = exerciseName,
            setNumber = exerciseSets.size + 1
        ))
        currentSets[exerciseId] = exerciseSets
        _sets.value = currentSets
    }

    fun updateSet(exerciseId: String, index: Int, weight: Double, reps: Int, rir: Int?) {
        val currentSets = _sets.value.toMutableMap()
        val exerciseSets = currentSets[exerciseId]?.toMutableList() ?: return
        if (index < exerciseSets.size) {
            exerciseSets[index] = exerciseSets[index].copy(
                weight = weight,
                reps = reps,
                rir = rir
            )
            currentSets[exerciseId] = exerciseSets
            _sets.value = currentSets
        }
    }

    fun finishSession(routineId: String, routineName: String) {
        val userId = auth.currentUser?.uid ?: "test_user"
        val allSets = _sets.value.values.flatten()
        val totalWeight = allSets.sumOf { it.weight * it.reps }
        
        val session = WorkoutSession(
            userId = userId,
            routineId = routineId,
            routineName = routineName,
            startTime = startTime,
            endTime = Timestamp.now(),
            totalWeightLifted = totalWeight
        )

        viewModelScope.launch {
            workoutRepository.saveWorkoutSession(session, allSets)
            timerJob?.cancel()
        }
    }
}

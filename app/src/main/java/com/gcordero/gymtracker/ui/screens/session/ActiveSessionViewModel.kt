package com.gcordero.gymtracker.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.ExerciseRepository
import com.gcordero.gymtracker.data.repository.RoutineRepository
import com.gcordero.gymtracker.data.repository.WorkoutRepository
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.gcordero.gymtracker.ui.navigation.SessionHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActiveSessionViewModel(
    private val exerciseRepository: ExerciseRepository = ExerciseRepository(),
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val routineRepository: RoutineRepository = RoutineRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _sets = MutableStateFlow<Map<String, List<SetRecord>>>(emptyMap())
    val sets: StateFlow<Map<String, List<SetRecord>>> = _sets.asStateFlow()

    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex: StateFlow<Int> = _currentExerciseIndex.asStateFlow()

    private val _currentSetNumber = MutableStateFlow(1)
    val currentSetNumber: StateFlow<Int> = _currentSetNumber.asStateFlow()

    private val _isResting = MutableStateFlow(false)
    val isResting: StateFlow<Boolean> = _isResting.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()

    private val _routineName = MutableStateFlow("")
    val routineName: StateFlow<String> = _routineName.asStateFlow()

    private val _lastSessionWeights = MutableStateFlow<Map<String, Double>>(emptyMap())
    val lastSessionWeights: StateFlow<Map<String, Double>> = _lastSessionWeights.asStateFlow()

    private val _isSessionSaved = MutableStateFlow(false)
    val isSessionSaved: StateFlow<Boolean> = _isSessionSaved.asStateFlow()

    // Controla qué pasa al terminar el descanso: "serie" = otra serie, "ejercicio" = siguiente ejercicio
    private val _restNextAction = MutableStateFlow("serie")
    val restNextAction: StateFlow<String> = _restNextAction.asStateFlow()

    fun setRestNextAction(action: String) {
        _restNextAction.value = action
    }

    private var timerJob: Job? = null
    private var restTimerJob: Job? = null
    private val startTime = Timestamp.now()

    fun startSession(routineId: String) {
        viewModelScope.launch {
            val routine = routineRepository.getRoutineById(routineId)
            _routineName.value = routine?.name ?: "Entrenamiento"
        }

        // Cargar pesos de la sesión anterior para prefill
        val userId = auth.currentUser?.uid ?: "test_user"
        viewModelScope.launch {
            val lastWeights = workoutRepository.getLastWeightsByRoutine(routineId, userId)
            _lastSessionWeights.value = lastWeights

            // Una vez que tengamos los ejercicios, prefill los sets iniciales
            exerciseRepository.getExercisesByRoutine(routineId).collect { exerciseList ->
                _exercises.value = exerciseList
                if (_sets.value.isEmpty()) {
                    val initialSets = mutableMapOf<String, List<SetRecord>>()
                    exerciseList.forEach { exercise ->
                        val lastWeight = lastWeights[exercise.id] ?: 0.0
                        initialSets[exercise.id] = listOf(
                            SetRecord(
                                exerciseId = exercise.id,
                                exerciseName = exercise.name,
                                setNumber = 1,
                                weight = lastWeight  // prefill con el peso de la última sesión
                            )
                        )
                    }
                    _sets.value = initialSets
                }
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
        _restTimerSeconds.value = 90 // Default 90s rest
        _isResting.value = true
        restTimerJob = viewModelScope.launch {
            while (_restTimerSeconds.value > 0) {
                delay(1000)
                _restTimerSeconds.value -= 1
            }
            onRestComplete()
        }
    }

    fun skipRest() {
        restTimerJob?.cancel()
        onRestComplete()
    }

    private fun onRestComplete() {
        _isResting.value = false
        _restTimerSeconds.value = 0

        if (_restNextAction.value == "ejercicio") {
            // El usuario eligió pasar al siguiente ejercicio
            nextExercise()
            _restNextAction.value = "serie" // reset para el próximo descanso
            return
        }

        // Avanzar a la siguiente serie del mismo ejercicio
        val exercise = _exercises.value.getOrNull(_currentExerciseIndex.value) ?: return
        val currentSets = _sets.value[exercise.id] ?: emptyList()
        val lastSet = currentSets.lastOrNull()

        _currentSetNumber.value += 1
        if (currentSets.size < _currentSetNumber.value) {
            addSet(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                initialWeight = lastSet?.weight ?: 0.0,
                initialReps = lastSet?.reps ?: 0,
                initialRir = lastSet?.rir  // RIR se hereda de la serie anterior
            )
        }
    }

    fun nextExercise() {
        if (_currentExerciseIndex.value < _exercises.value.size - 1) {
            _currentExerciseIndex.value += 1
            _currentSetNumber.value = 1
        }
    }

    fun addSet(
        exerciseId: String,
        exerciseName: String,
        initialWeight: Double = 0.0,
        initialReps: Int = 0,
        initialRir: Int? = null
    ) {
        val currentSets = _sets.value.toMutableMap()
        val exerciseSets = currentSets[exerciseId]?.toMutableList() ?: mutableListOf()
        exerciseSets.add(
            SetRecord(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                setNumber = exerciseSets.size + 1,
                weight = initialWeight,
                reps = initialReps,
                rir = initialRir
            )
        )
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

    fun finishSession(routineId: String) {
        val userId = auth.currentUser?.uid ?: "test_user"
        val allSets = _sets.value.values.flatten()
        val totalWeight = allSets.sumOf { it.weight * it.reps }
        val name = _routineName.value.ifEmpty { "Entrenamiento" }

        val session = WorkoutSession(
            userId = userId,
            routineId = routineId,
            routineName = name,
            startTime = startTime,
            endTime = Timestamp.now(),
            totalWeightLifted = totalWeight
        )

        viewModelScope.launch {
            val sessionId = workoutRepository.saveWorkoutSession(session, allSets)
            SessionHolder.selectedSession = session.copy(id = sessionId)
            timerJob?.cancel()
            // Activar DESPUES de que todo esté listo para que la UI navegue
            _isSessionSaved.value = true
        }
    }
}

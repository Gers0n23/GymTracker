package com.gcordero.gymtracker.ui.screens.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.ExerciseRepository
import com.gcordero.gymtracker.data.repository.RoutineRepository
import com.gcordero.gymtracker.data.util.RoutineExportUtil
import com.gcordero.gymtracker.domain.model.Routine
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ImportState {
    object Idle : ImportState()
    object Success : ImportState()
    data class Error(val message: String) : ImportState()
}

class RoutinesViewModel(
    private val repository: RoutineRepository = RoutineRepository(),
    private val exerciseRepository: ExerciseRepository = ExerciseRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    init {
        loadRoutines()
    }

    private fun loadRoutines() {
        val userId = auth.currentUser?.uid ?: "test_user"
        viewModelScope.launch {
            _isLoading.value = true
            repository.getRoutines(userId).collect {
                _routines.value = it
                _isLoading.value = false
            }
        }
    }

    fun addRoutine(name: String, description: String, muscleGroups: List<String> = emptyList()) {
        val userId = auth.currentUser?.uid ?: "test_user"
        val newRoutine = Routine(
            userId = userId,
            name = name,
            description = description,
            muscleGroups = muscleGroups
        )
        viewModelScope.launch {
            repository.addRoutine(newRoutine)
        }
    }

    fun updateRoutine(routine: Routine) {
        viewModelScope.launch {
            repository.updateRoutine(routine)
        }
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
        }
    }

    fun importRoutine(text: String) {
        val parsed = RoutineExportUtil.parse(text)
        if (parsed == null) {
            _importState.value = ImportState.Error("No se encontró una rutina válida en el texto. Asegúrate de pegar el mensaje completo.")
            return
        }
        val userId = auth.currentUser?.uid ?: "test_user"
        viewModelScope.launch {
            runCatching {
                val routineId = repository.addRoutine(
                    parsed.routine.copy(userId = userId)
                )
                parsed.exercises.forEachIndexed { index, exercise ->
                    exerciseRepository.addExercise(
                        exercise.copy(routineId = routineId, order = index)
                    )
                }
            }.onSuccess {
                _importState.value = ImportState.Success
            }.onFailure {
                _importState.value = ImportState.Error("Error al importar: ${it.message}")
            }
        }
    }

    fun clearImportState() {
        _importState.value = ImportState.Idle
    }
}

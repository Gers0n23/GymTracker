package com.gcordero.gymtracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.WorkoutRepository
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.domain.model.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionDetailUiState(
    val session: WorkoutSession? = null,
    val setsByExercise: Map<String, List<SetRecord>> = emptyMap(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class SessionDetailViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    // Mutable working copy of sets (indexed by exerciseName then list index)
    private val _editableSets = MutableStateFlow<Map<String, List<SetRecord>>>(emptyMap())
    val editableSets: StateFlow<Map<String, List<SetRecord>>> = _editableSets.asStateFlow()

    // Sets de la sesión anterior (para comparativa)
    private val _previousSets = MutableStateFlow<Map<String, List<SetRecord>>>(emptyMap())
    val previousSets: StateFlow<Map<String, List<SetRecord>>> = _previousSets.asStateFlow()

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val sets = workoutRepository.getSetsBySession(sessionId)
                val grouped = sets.groupBy { it.exerciseName }
                _editableSets.value = grouped
                _uiState.value = _uiState.value.copy(
                    setsByExercise = grouped,
                    isLoading = false
                )

                // Cargar sesión anterior para comparativa (no crítico si falla)
                val session = _uiState.value.session
                if (session != null && session.routineId.isNotEmpty()) {
                    try {
                        val prevSets = workoutRepository.getPreviousSessionSets(
                            session.routineId, session.userId, sessionId
                        )
                        _previousSets.value = prevSets.groupBy { it.exerciseName }
                    } catch (_: Exception) { /* comparativa no disponible */ }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun setSession(session: WorkoutSession) {
        _uiState.value = _uiState.value.copy(session = session)
    }

    fun updateSetWeight(exerciseName: String, index: Int, weight: Double) {
        val current = _editableSets.value.toMutableMap()
        val list = current[exerciseName]?.toMutableList() ?: return
        if (index < list.size) {
            list[index] = list[index].copy(weight = weight)
            current[exerciseName] = list
            _editableSets.value = current
        }
    }

    fun updateSetReps(exerciseName: String, index: Int, reps: Int) {
        val current = _editableSets.value.toMutableMap()
        val list = current[exerciseName]?.toMutableList() ?: return
        if (index < list.size) {
            list[index] = list[index].copy(reps = reps)
            current[exerciseName] = list
            _editableSets.value = current
        }
    }

    fun updateSetRir(exerciseName: String, index: Int, rir: Int?) {
        val current = _editableSets.value.toMutableMap()
        val list = current[exerciseName]?.toMutableList() ?: return
        if (index < list.size) {
            list[index] = list[index].copy(rir = rir)
            current[exerciseName] = list
            _editableSets.value = current
        }
    }

    fun saveChanges() {
        val session = _uiState.value.session ?: return
        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val allSets = _editableSets.value.values.flatten()

                // Update each set in Firestore
                allSets.forEach { set ->
                    workoutRepository.updateSet(set)
                }

                // Recalculate total weight and update session
                val newTotal = allSets.sumOf { it.weight * it.reps }
                val updatedSession = session.copy(totalWeightLifted = newTotal)
                workoutRepository.updateSession(updatedSession)

                _uiState.value = _uiState.value.copy(
                    session = updatedSession,
                    isSaving = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message
                )
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}

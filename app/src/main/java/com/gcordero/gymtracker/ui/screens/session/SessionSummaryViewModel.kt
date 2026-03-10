package com.gcordero.gymtracker.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.WorkoutRepository
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.domain.model.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SessionSummaryViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository()
) : ViewModel() {

    private val _sets = MutableStateFlow<List<SetRecord>>(emptyList())
    val sets: StateFlow<List<SetRecord>> = _sets

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    fun loadSets(sessionId: String) {
        viewModelScope.launch {
            _sets.value = workoutRepository.getSetsBySession(sessionId)
        }
    }

    fun saveFinal(session: WorkoutSession, rpe: Int, sleepQuality: Int, energyLevel: Int, previousDayProtein: Int? = null) {
        viewModelScope.launch {
            _isSaving.value = true
            val updated = session.copy(
                rpe = rpe,
                sleepQuality = sleepQuality,
                energyLevel = energyLevel,
                proteinConsumedPreviousDay = previousDayProtein
            )
            workoutRepository.updateSession(updated)
            _isSaving.value = false
        }
    }
}

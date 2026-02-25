package com.gcordero.gymtracker.ui.screens.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.RoutineRepository
import com.gcordero.gymtracker.domain.model.Routine
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RoutinesViewModel(
    private val repository: RoutineRepository = RoutineRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
}

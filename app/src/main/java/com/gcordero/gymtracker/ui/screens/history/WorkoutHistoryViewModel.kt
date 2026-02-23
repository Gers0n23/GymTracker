package com.gcordero.gymtracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.WorkoutRepository
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class WorkoutHistoryViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _allSessions = MutableStateFlow<List<WorkoutSession>>(emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Filter state
    private val _selectedRoutines = MutableStateFlow<Set<String>>(emptySet())
    val selectedRoutines: StateFlow<Set<String>> = _selectedRoutines.asStateFlow()

    private val _dateFrom = MutableStateFlow<Date?>(null)
    val dateFrom: StateFlow<Date?> = _dateFrom.asStateFlow()

    private val _dateTo = MutableStateFlow<Date?>(null)
    val dateTo: StateFlow<Date?> = _dateTo.asStateFlow()

    val availableRoutines: StateFlow<List<String>> = _allSessions
        .map { list -> list.map { it.routineName }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<WorkoutSession>> = combine(
        _allSessions, _selectedRoutines, _dateFrom, _dateTo
    ) { all, routines, from, to ->
        all.filter { session ->
            val matchRoutine = routines.isEmpty() || routines.contains(session.routineName)
            val sessionDate = session.startTime.toDate()
            val matchFrom = from == null || !sessionDate.before(from)
            val matchTo = to == null || !sessionDate.after(adjustToEndOfDay(to))
            matchRoutine && matchFrom && matchTo
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasActiveFilters: StateFlow<Boolean> = combine(
        _selectedRoutines, _dateFrom, _dateTo
    ) { routines, from, to ->
        routines.isNotEmpty() || from != null || to != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadSessions()
    }

    private fun loadSessions() {
        val userId = auth.currentUser?.uid ?: "test_user"
        viewModelScope.launch {
            workoutRepository.getSessions(userId).collect { list ->
                _allSessions.value = list
                _isLoading.value = false
            }
        }
    }

    fun toggleRoutineFilter(routineName: String) {
        _selectedRoutines.value = _selectedRoutines.value.toMutableSet().apply {
            if (contains(routineName)) remove(routineName) else add(routineName)
        }
    }

    fun setDateFrom(date: Date?) {
        _dateFrom.value = date
    }

    fun setDateTo(date: Date?) {
        _dateTo.value = date
    }

    fun clearFilters() {
        _selectedRoutines.value = emptySet()
        _dateFrom.value = null
        _dateTo.value = null
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            workoutRepository.deleteSession(sessionId)
        }
    }

    private fun adjustToEndOfDay(date: Date?): Date? {
        if (date == null) return null
        val cal = java.util.Calendar.getInstance().apply {
            time = date
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }
        return cal.time
    }
}

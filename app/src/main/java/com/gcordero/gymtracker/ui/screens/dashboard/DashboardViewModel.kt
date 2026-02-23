package com.gcordero.gymtracker.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.util.DataPopulator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PopulateState {
    object Idle : PopulateState()
    object Loading : PopulateState()
    object Success : PopulateState()
    data class Error(val message: String) : PopulateState()
}

class DashboardViewModel : ViewModel() {

    private val _populateState = MutableStateFlow<PopulateState>(PopulateState.Idle)
    val populateState: StateFlow<PopulateState> = _populateState

    fun populateData(userId: String) {
        if (_populateState.value is PopulateState.Loading) return
        viewModelScope.launch {
            _populateState.value = PopulateState.Loading
            val result = DataPopulator.populateInitialData(userId)
            _populateState.value = if (result.isSuccess) {
                PopulateState.Success
            } else {
                PopulateState.Error(result.exceptionOrNull()?.localizedMessage ?: "Error desconocido")
            }
        }
    }

    fun resetState() {
        _populateState.value = PopulateState.Idle
    }
}

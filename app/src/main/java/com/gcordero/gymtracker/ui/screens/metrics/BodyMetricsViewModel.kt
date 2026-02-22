package com.gcordero.gymtracker.ui.screens.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.BodyMetricsRepository
import com.gcordero.gymtracker.domain.model.BodyMetric
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BodyMetricsViewModel(
    private val repository: BodyMetricsRepository = BodyMetricsRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _metrics = MutableStateFlow<List<BodyMetric>>(emptyList())
    val metrics: StateFlow<List<BodyMetric>> = _metrics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMetrics()
    }

    private fun loadMetrics() {
        val userId = auth.currentUser?.uid ?: "test_user"
        viewModelScope.launch {
            _isLoading.value = true
            repository.getBodyMetrics(userId).collect {
                _metrics.value = it
                _isLoading.value = false
            }
        }
    }

    fun addMetric(weight: Double, fat: Double?, muscle: Double?) {
        val userId = auth.currentUser?.uid ?: "test_user"
        // Simple IMC calculation (assuming height is 1.75m for now or fetched from user profile)
        // In a real app, height would be in User model
        val height = 1.75 
        val imc = weight / (height * height)
        
        val newMetric = BodyMetric(
            userId = userId,
            weightKg = weight,
            fatPercentage = fat,
            musclePercentage = muscle,
            imc = imc
        )
        viewModelScope.launch {
            repository.addBodyMetric(newMetric)
        }
    }
}

package com.vayu.health.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.health.data.HealthMetricsRepository
import com.vayu.health.data.model.HealthDashboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HealthDashboardViewModel(
    private val repository: HealthMetricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthDashboardState(isLoading = true))
    val uiState: StateFlow<HealthDashboardState> = _uiState.asStateFlow()

    init {
        loadMetrics()
    }

    fun loadMetrics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.getTodayDashboardState()
            _uiState.value = result
        }
    }

    fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
            .withZone(ZoneId.systemDefault())
        return formatter.format(Instant.now()).uppercase()
    }

    fun formatTime(instant: Instant?): String {
        if (instant == null) return "Time unavailable"
        val formatter = DateTimeFormatter.ofPattern("h:mm a")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }

    fun formatDuration(minutes: Long?): String {
        if (minutes == null || minutes <= 0) return "Data unavailable"
        val hours = minutes / 60
        val remaining = minutes % 60
        return if (hours > 0) "${hours}h ${remaining}m" else "${remaining}m"
    }
}

package com.vayu.health.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.health.data.HealthMetricsRepository
import com.vayu.health.data.HealthSourcePreferences
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.data.model.MetricDetailType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HealthDashboardViewModel(
    private val repository: HealthMetricsRepository,
    private val sourcePreferences: HealthSourcePreferences? = null
) : ViewModel() {

    private var currentSource: String? = sourcePreferences?.getSelectedSource()

    private val _uiState = MutableStateFlow(
        HealthDashboardState(
            isLoading = true,
            selectedSource = currentSource
        )
    )
    val uiState: StateFlow<HealthDashboardState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _activeMetricDetail = MutableStateFlow<MetricDetailType?>(null)
    val activeMetricDetail: StateFlow<MetricDetailType?> = _activeMetricDetail.asStateFlow()

    private val _isFirstRun = MutableStateFlow(sourcePreferences?.isFirstRun() ?: false)
    val isFirstRun: StateFlow<Boolean> = _isFirstRun.asStateFlow()

    init {
        loadMetrics()
        startLiveHeartRatePolling()
    }

    private fun startLiveHeartRatePolling() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(15_000)
                if (_selectedDate.value == LocalDate.now() && !_uiState.value.isLoading) {
                    val liveHr = repository.getLiveHeartRate(currentSource)
                    if (liveHr != null && liveHr.samples.isNotEmpty()) {
                        val currentHr = _uiState.value.heartRate
                        if (currentHr == null || liveHr.latestBpm != currentHr.latestBpm || liveHr.samples.size != currentHr.samples.size) {
                            _uiState.value = _uiState.value.copy(heartRate = liveHr)
                        }
                    }
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun loadMetrics(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val date = _selectedDate.value
            val result = repository.getTodayDashboardState(sourceFilter = currentSource, targetDate = date)
            val targetMins = sourcePreferences?.getSleepTargetMinutes() ?: 480
            val calculatedSleepScore = com.vayu.health.data.model.HealthDataCalculations.calculateSleepScore(
                sleep = result.sleep,
                targetMinutes = targetMins,
                restingHeartRateBpm = result.restingHeartRate?.beatsPerMinute,
                nocturnalHeartRateSamples = result.heartRate?.samples ?: emptyList()
            )
            _uiState.value = result.copy(sleepScore = calculatedSleepScore)
        }
    }

    fun updateSleepTarget(minutes: Int) {
        sourcePreferences?.setSleepTargetMinutes(minutes)
        loadMetrics()
    }

    fun updateGoogleHealthRefreshToken(refreshToken: String) {
        sourcePreferences?.setGoogleHealthRefreshToken(refreshToken)
        loadMetrics(forceRefresh = true)
    }

    fun setGoogleHealthEnabled(enabled: Boolean) {
        sourcePreferences?.setGoogleHealthEnabled(enabled)
        loadMetrics(forceRefresh = true)
    }

    fun openMetricDetail(metric: MetricDetailType) {
        _activeMetricDetail.value = metric
    }

    fun closeMetricDetail() {
        _activeMetricDetail.value = null
    }

    fun selectPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
        loadMetrics()
    }

    fun selectNextDay() {
        if (_selectedDate.value.isBefore(LocalDate.now())) {
            _selectedDate.value = _selectedDate.value.plusDays(1)
            loadMetrics()
        }
    }

    fun setSourceFilter(source: String?) {
        currentSource = source
        sourcePreferences?.setSelectedSource(source)
        sourcePreferences?.setFirstRunCompleted()
        _isFirstRun.value = false
        loadMetrics()
    }

    fun dismissFirstRun() {
        sourcePreferences?.setFirstRunCompleted()
        _isFirstRun.value = false
    }

    fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
            .withZone(ZoneId.systemDefault())
        return formatter.format(Instant.now()).uppercase()
    }

    fun getBaroHeaderDate(): String {
        val today = LocalDate.now()
        val sel = _selectedDate.value
        val dMmm = DateTimeFormatter.ofPattern("d MMM")
        return when (sel) {
            today -> "Today, ${dMmm.format(sel)}"
            today.minusDays(1) -> "Yesterday, ${dMmm.format(sel)}"
            else -> DateTimeFormatter.ofPattern("EEE, d MMM").format(sel)
        }
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

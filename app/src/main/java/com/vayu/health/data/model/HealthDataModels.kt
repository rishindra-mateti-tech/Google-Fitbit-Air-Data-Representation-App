package com.vayu.health.data.model

import java.time.Instant

/**
 * STRICT PHILOSOPHY: NO INVENTED METRICS.
 * All models hold pure pass-through readings directly from Health Connect.
 * If a metric was not recorded or reported by Fitbit Air, it is null ("Data unavailable").
 */

enum class SleepStageType {
    DEEP,
    REM,
    LIGHT,
    AWAKE,
    UNKNOWN
}

data class SleepStageSegment(
    val stage: SleepStageType,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Long
)

data class SleepMetric(
    val totalDurationMinutes: Long,
    val deepMinutes: Long,
    val remMinutes: Long,
    val lightMinutes: Long,
    val awakeMinutes: Long,
    val startTime: Instant,
    val endTime: Instant,
    val stages: List<SleepStageSegment>,
    val sourcePackage: String = "com.fitbit.FitbitMobile"
)

data class RestingHeartRateMetric(
    val beatsPerMinute: Long,
    val timestamp: Instant,
    val sourcePackage: String = "com.fitbit.FitbitMobile"
)

data class HrvMetric(
    val rmssdMillis: Double,
    val timestamp: Instant,
    val sourcePackage: String = "com.fitbit.FitbitMobile"
)

data class HeartRateMetric(
    val latestBpm: Long?,
    val minBpm: Long?,
    val maxBpm: Long?,
    val samplesCount: Int,
    val timestamp: Instant?,
    val sourcePackage: String = "com.fitbit.FitbitMobile"
)

data class ActivityMetric(
    val steps: Long?,
    val activeCaloriesKcal: Double?,
    val totalCaloriesKcal: Double?,
    val distanceMeters: Double?,
    val lastUpdated: Instant?
)

data class RespiratoryMetric(
    val breathsPerMinute: Double?,
    val spo2Percentage: Double?,
    val skinTempDeviationCelsius: Double?,
    val timestamp: Instant?
)

data class RawHealthRecord(
    val recordType: String,
    val valueSummary: String,
    val startTime: Instant,
    val endTime: Instant?,
    val clientPackage: String,
    val recordId: String
)

data class HealthDashboardState(
    val isLoading: Boolean = false,
    val isHealthConnectAvailable: Boolean = true,
    val hasPermissions: Boolean = false,
    val lastRefreshTime: Instant? = null,
    val sleep: SleepMetric? = null,
    val restingHeartRate: RestingHeartRateMetric? = null,
    val hrv: HrvMetric? = null,
    val heartRate: HeartRateMetric? = null,
    val activity: ActivityMetric? = null,
    val respiratory: RespiratoryMetric? = null,
    val rawRecords: List<RawHealthRecord> = emptyList(),
    val errorMessage: String? = null
)

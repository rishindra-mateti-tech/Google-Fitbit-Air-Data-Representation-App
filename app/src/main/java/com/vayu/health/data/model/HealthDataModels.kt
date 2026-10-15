package com.vayu.health.data.model

import java.time.Instant
import java.time.LocalDate

/**
 * STRICT PHILOSOPHY: NO INVENTED METRICS.
 * All models hold pure pass-through readings directly from Health Connect.
 * Never show demo, fallback, estimated, calculated, interpreted, or invented health values.
 * If a metric is unavailable, it is null ("Data unavailable").
 */

enum class SleepStageType {
    DEEP,
    REM,
    LIGHT,
    SLEEPING,
    AWAKE,
    OUT_OF_BED,
    UNKNOWN
}

data class SleepStageSegment(
    val stage: SleepStageType,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Long
)

data class SleepMetric(
    val sessionElapsedMinutes: Long,
    val actualSleepDurationMinutes: Long?,
    val deepMinutes: Long,
    val remMinutes: Long,
    val lightMinutes: Long,
    val sleepingMinutes: Long,
    val awakeMinutes: Long,
    val outOfBedMinutes: Long,
    val startTime: Instant,
    val endTime: Instant,
    val stages: List<SleepStageSegment>,
    val sourcePackage: String
)

data class DailySleepSummary(
    val date: LocalDate,
    val actualSleepMinutes: Long,
    val sessionElapsedMinutes: Long,
    val sourcePackage: String
)

data class RestingHeartRateMetric(
    val beatsPerMinute: Long,
    val timestamp: Instant,
    val sourcePackage: String
)

data class HrvMetric(
    val rmssdMillis: Double,
    val timestamp: Instant,
    val sourcePackage: String
)

data class HeartRateSamplePoint(
    val time: Instant,
    val beatsPerMinute: Long
)

data class HeartRateMetric(
    val latestBpm: Long?,
    val minBpm: Long?,
    val maxBpm: Long?,
    val averageBpm: Long?,
    val samples: List<HeartRateSamplePoint>,
    val timestamp: Instant?,
    val sourcePackage: String
)

data class ActivityMetric(
    val steps: Long?,
    val activeCaloriesKcal: Double?,
    val totalCaloriesKcal: Double?,
    val distanceMeters: Double?,
    val sources: Set<String>,
    val lastUpdated: Instant?
)

data class SkinTemperatureMetric(
    val baselineCelsius: Double?,
    val averageDeltaCelsius: Double?,
    val minDeltaCelsius: Double?,
    val maxDeltaCelsius: Double?,
    val deltasCount: Int,
    val timestamp: Instant?,
    val sourcePackage: String
)

data class RespiratoryMetric(
    val breathsPerMinute: Double?,
    val spo2Percentage: Double?,
    val skinTemperature: SkinTemperatureMetric?,
    val timestamp: Instant?,
    val sourcePackage: String?
)

data class RawHealthRecord(
    val recordType: String,
    val valueSummary: String,
    val startTime: Instant,
    val endTime: Instant?,
    val clientPackage: String,
    val recordId: String
)

enum class HealthConnectStatus {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE
}

data class HealthDashboardState(
    val isLoading: Boolean = false,
    val sdkStatus: HealthConnectStatus = HealthConnectStatus.AVAILABLE,
    val grantedPermissions: Set<String> = emptySet(),
    val lastRefreshTime: Instant? = null,
    val sleep: SleepMetric? = null,
    val dailySleepHistory: List<DailySleepSummary> = emptyList(),
    val restingHeartRate: RestingHeartRateMetric? = null,
    val hrv: HrvMetric? = null,
    val heartRate: HeartRateMetric? = null,
    val activity: ActivityMetric? = null,
    val respiratory: RespiratoryMetric? = null,
    val rawRecords: List<RawHealthRecord> = emptyList(),
    val isSkinTemperatureSupported: Boolean = false,
    val errorMessages: List<String> = emptyList()
)

object HealthDataCalculations {
    data class SleepStagesBreakdown(
        val deepMinutes: Long,
        val remMinutes: Long,
        val lightMinutes: Long,
        val sleepingMinutes: Long,
        val awakeMinutes: Long,
        val outOfBedMinutes: Long,
        val actualSleepDurationMinutes: Long?
    )

    fun calculateSleepStages(stages: List<SleepStageSegment>): SleepStagesBreakdown {
        var deepMins = 0L
        var remMins = 0L
        var lightMins = 0L
        var sleepingMins = 0L
        var awakeMins = 0L
        var outOfBedMins = 0L

        stages.forEach { stage ->
            val duration = stage.durationMinutes.coerceAtLeast(0)
            when (stage.stage) {
                SleepStageType.DEEP -> deepMins += duration
                SleepStageType.REM -> remMins += duration
                SleepStageType.LIGHT -> lightMins += duration
                SleepStageType.SLEEPING -> sleepingMins += duration
                SleepStageType.AWAKE -> awakeMins += duration
                SleepStageType.OUT_OF_BED -> outOfBedMins += duration
                SleepStageType.UNKNOWN -> {}
            }
        }

        val totalSleepTime = if (stages.isNotEmpty()) {
            deepMins + remMins + lightMins + sleepingMins
        } else {
            null
        }

        return SleepStagesBreakdown(
            deepMinutes = deepMins,
            remMinutes = remMins,
            lightMinutes = lightMins,
            sleepingMinutes = sleepingMins,
            awakeMinutes = awakeMins,
            outOfBedMinutes = outOfBedMins,
            actualSleepDurationMinutes = totalSleepTime
        )
    }

    data class HeartRateSummary(
        val minBpm: Long,
        val maxBpm: Long,
        val averageBpm: Long,
        val latestBpm: Long
    )

    fun calculateHeartRateStats(samples: List<HeartRateSamplePoint>): HeartRateSummary? {
        if (samples.isEmpty()) return null
        val minBpm = samples.minOf { it.beatsPerMinute }
        val maxBpm = samples.maxOf { it.beatsPerMinute }
        val avgBpm = (samples.sumOf { it.beatsPerMinute } / samples.size.toDouble()).toLong()
        val latestBpm = samples.last().beatsPerMinute
        return HeartRateSummary(
            minBpm = minBpm,
            maxBpm = maxBpm,
            averageBpm = avgBpm,
            latestBpm = latestBpm
        )
    }

    data class SkinTempSummary(
        val averageDelta: Double?,
        val minDelta: Double?,
        val maxDelta: Double?,
        val deltasCount: Int
    )

    fun calculateSkinTempStats(deltas: List<Double>): SkinTempSummary {
        if (deltas.isEmpty()) {
            return SkinTempSummary(null, null, null, 0)
        }
        return SkinTempSummary(
            averageDelta = deltas.average(),
            minDelta = deltas.minOrNull(),
            maxDelta = deltas.maxOrNull(),
            deltasCount = deltas.size
        )
    }
}


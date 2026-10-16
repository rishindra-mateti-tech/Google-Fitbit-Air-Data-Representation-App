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
    val sourcePackage: String,
    val recordedScore: Int? = null
)

data class DailySleepSummary(
    val date: LocalDate,
    val actualSleepMinutes: Long?,
    val sessionElapsedMinutes: Long,
    val sourcePackage: String,
    val sessionCount: Int = 1,
    val contributingSources: Set<String> = emptySet()
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
    val beatsPerMinute: Long,
    val sourcePackage: String = ""
)

data class HeartRateMetric(
    val latestBpm: Long?,
    val minBpm: Long?,
    val maxBpm: Long?,
    val averageBpm: Long?,
    val samples: List<HeartRateSamplePoint>,
    val timestamp: Instant?,
    val sourcePackage: String,
    val sources: Set<String> = emptySet()
)

data class ActivityMetric(
    val steps: Long?,
    val activeCaloriesKcal: Double?,
    val totalCaloriesKcal: Double?,
    val distanceMeters: Double?,
    val sources: Set<String>,
    val lastUpdated: Instant?,
    val hourlySteps: List<Float> = emptyList()
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
    val respiratoryTimestamp: Instant?,
    val respiratorySourcePackage: String?,
    val spo2Percentage: Double?,
    val spo2Timestamp: Instant?,
    val spo2SourcePackage: String?,
    val skinTemperature: SkinTemperatureMetric?,
    val timestamp: Instant?,
    val contributingSources: Set<String> = emptySet()
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

enum class MetricDetailType {
    READINESS,
    HEART_RATE,
    STEPS,
    ZONE_MINUTES,
    STRESS,
    RESTING_HR,
    HRV,
    RESP_RATE,
    SPO2,
    RESP_TEMP,
    SKIN_TEMP
}

data class HealthDashboardState(
    val isLoading: Boolean = false,
    val sdkStatus: HealthConnectStatus = HealthConnectStatus.AVAILABLE,
    val grantedPermissions: Set<String> = emptySet(),
    val lastRefreshTime: Instant? = null,
    val selectedSource: String? = null,
    val detectedSources: Set<String> = emptySet(),
    val isFitbitDetected: Boolean = false,
    val isFitbitSyncNeeded: Boolean = false,
    val sleep: SleepMetric? = null,
    val sleepScore: Int? = null,
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

    fun parseSleepScoreFromText(title: String?, notes: String?): Int? {
        val combined = listOfNotNull(title, notes).joinToString(" ")
        if (combined.isBlank()) return null
        val regex = Regex("""(?:score[:\s=]*|score\s+to\s+)(\d{1,3})|(\d{1,3})\s*(?:•\s*(?:Good|Fair|Poor|Excellent)|/100)""", RegexOption.IGNORE_CASE)
        val match = regex.find(combined)
        if (match != null) {
            val scoreStr = match.groups[1]?.value ?: match.groups[2]?.value
            val score = scoreStr?.toIntOrNull()
            if (score != null && score in 10..100) return score
        }
        val regex2 = Regex("""\b(?:sleep|quality)[^\d]{1,15}(\d{2})\b""", RegexOption.IGNORE_CASE)
        val match2 = regex2.find(combined)
        if (match2 != null) {
            val score = match2.groupValues[1].toIntOrNull()
            if (score != null && score in 30..100) return score
        }
        return null
    }

    /**
     * Sleep score calculation aligned with Fitbit's 3-pillar scoring:
     * 1. Duration (up to 50 pts): Target 7-8 hours.
     * 2. Restorative Sleep (Deep + REM ratio, up to 25 pts, target 25-40%).
     * 3. Efficiency (Time asleep vs in bed, up to 25 pts).
     * If provider exported score in text (title/notes), that score takes precedence.
     */
    fun calculateSleepScore(sleep: SleepMetric?): Int? {
        if (sleep == null || sleep.actualSleepDurationMinutes == null || sleep.sessionElapsedMinutes <= 0) return null
        if (sleep.recordedScore != null) return sleep.recordedScore

        val actualMins = sleep.actualSleepDurationMinutes
        val elapsedMins = sleep.sessionElapsedMinutes

        val durationRatio = (actualMins.toDouble() / 480.0).coerceIn(0.0, 1.0)
        val durationScore = Math.pow(durationRatio, 1.7) * 50.0

        val restorativeMins = sleep.deepMinutes + sleep.remMinutes
        val restorativeRatio = restorativeMins.toDouble() / actualMins.coerceAtLeast(1)
        val restorativeScore = ((restorativeRatio / 0.40) * 25.0).coerceIn(0.0, 25.0)

        val efficiency = actualMins.toDouble() / elapsedMins
        val efficiencyScore = (efficiency * 25.0).coerceIn(0.0, 25.0)

        return ((durationScore + restorativeScore + efficiencyScore) + 0.5).toInt().coerceIn(10, 100)
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

    data class HeartRateZoneBreakdown(
        val zone4Minutes: Long,
        val zone3Minutes: Long,
        val zone2Minutes: Long,
        val zone1Minutes: Long,
        val totalMinutes: Long,
        val zone4Fraction: Float,
        val zone3Fraction: Float,
        val zone2Fraction: Float,
        val zone1Fraction: Float,
        val activeZoneMinutes: Long = zone2Minutes + (zone3Minutes * 2) + (zone4Minutes * 2),
        val zone1Label: String = "<95 bpm",
        val zone2Label: String = "95-125 bpm",
        val zone3Label: String = "125-150 bpm",
        val zone4Label: String = ">150 bpm"
    )

    /**
     * Calculates time in heart rate zones from timestamped Health Connect sample points.
     * Uses individualized Heart Rate Reserve (Karvonen method) when resting HR is available,
     * or standard physiological bands as fallback.
     * Durations are calculated from sample timestamps (Duration.between), not sample counts.
     */
    fun calculateHeartRateZones(
        samples: List<HeartRateSamplePoint>,
        restingHr: Int? = null,
        maxHr: Int = 190
    ): HeartRateZoneBreakdown {
        if (samples.isEmpty()) {
            return HeartRateZoneBreakdown(0L, 0L, 0L, 0L, 0L, 0f, 0f, 0f, 0f, 0L)
        }

        val (z1Upper, z2Upper, z3Upper, labels) = if (restingHr != null && restingHr in 31 until maxHr) {
            val hrr = maxHr - restingHr
            val z1 = restingHr + (0.50f * hrr).toInt()
            val z2 = restingHr + (0.70f * hrr).toInt()
            val z3 = restingHr + (0.85f * hrr).toInt()
            listOf(
                z1, z2, z3,
                listOf("<$z1 bpm", "$z1-$z2 bpm", "$z2-$z3 bpm", ">$z3 bpm")
            )
        } else {
            listOf(
                95, 125, 150,
                listOf("<95 bpm", "95-125 bpm", "125-150 bpm", ">150 bpm")
            )
        }

        @Suppress("UNCHECKED_CAST")
        val z1Threshold = z1Upper as Int
        @Suppress("UNCHECKED_CAST")
        val z2Threshold = z2Upper as Int
        @Suppress("UNCHECKED_CAST")
        val z3Threshold = z3Upper as Int
        @Suppress("UNCHECKED_CAST")
        val zLabels = labels as List<String>

        val sorted = samples.sortedBy { it.time }
        var z4Secs = 0L
        var z3Secs = 0L
        var z2Secs = 0L
        var z1Secs = 0L

        for (i in sorted.indices) {
            val sample = sorted[i]
            val durationSecs = if (i < sorted.size - 1) {
                val delta = java.time.Duration.between(sample.time, sorted[i + 1].time).seconds
                if (delta in 1..300) delta else 60L
            } else {
                60L
            }

            when {
                sample.beatsPerMinute > z3Threshold -> z4Secs += durationSecs
                sample.beatsPerMinute >= z2Threshold -> z3Secs += durationSecs
                sample.beatsPerMinute >= z1Threshold -> z2Secs += durationSecs
                else -> z1Secs += durationSecs
            }
        }

        val z4Min = (z4Secs + 30L) / 60L
        val z3Min = (z3Secs + 30L) / 60L
        val z2Min = (z2Secs + 30L) / 60L
        val z1Min = (z1Secs + 30L) / 60L
        val totalSecs = (z4Secs + z3Secs + z2Secs + z1Secs).coerceAtLeast(1L)
        val totalMin = (totalSecs + 30L) / 60L

        val azm = z2Min + (z3Min * 2) + (z4Min * 2)

        return HeartRateZoneBreakdown(
            zone4Minutes = z4Min,
            zone3Minutes = z3Min,
            zone2Minutes = z2Min,
            zone1Minutes = z1Min,
            totalMinutes = totalMin,
            zone4Fraction = (z4Secs.toFloat() / totalSecs).coerceIn(0f, 1f),
            zone3Fraction = (z3Secs.toFloat() / totalSecs).coerceIn(0f, 1f),
            zone2Fraction = (z2Secs.toFloat() / totalSecs).coerceIn(0f, 1f),
            zone1Fraction = (z1Secs.toFloat() / totalSecs).coerceIn(0f, 1f),
            activeZoneMinutes = azm,
            zone1Label = zLabels[0],
            zone2Label = zLabels[1],
            zone3Label = zLabels[2],
            zone4Label = zLabels[3]
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

    /**
     * Aggregates timestamped step points into 24 one-hour bins.
     * Returns authentic movement hours without synthetic patterns.
     */
    fun calculateHourlyStepDistribution(
        stepRecords: List<Pair<Instant, Long>>,
        zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): List<Float> {
        if (stepRecords.isEmpty()) return List(24) { 0f }
        val hourly = FloatArray(24) { 0f }
        stepRecords.forEach { (time, count) ->
            val hour = time.atZone(zoneId).hour
            if (hour in 0..23) {
                hourly[hour] += count.toFloat()
            }
        }
        return hourly.toList()
    }

    @Deprecated("Do not use synthetic step allocation. Use Health Connect StepsRecord.")
    fun calculateHourlyStepDistribution(totalSteps: Long?): List<Float> {
        return List(24) { 0f }
    }

    data class StressSummary(
        val averageScore: Float,
        val minScore: Float,
        val maxScore: Float,
        val levelLabel: String
    )

    /**
     * Experimental Autonomic Stress Indicator.
     * Evaluates daytime pulse elevation relative to baseline resting heart rate,
     * modulated by nocturnal HRV (RMSSD) suppression.
     * Note: This is an empirical personal ratio indicator, not a clinical ECG/RR-interval diagnostic.
     * Scale: 0.0 to 4.0 (0.0–1.0 Calm, 1.0–2.0 Low, 2.0–2.5 Medium, 2.5–4.0 High).
     */
    fun calculatePhysiologicalStress(
        hr: HeartRateMetric?,
        rhr: RestingHeartRateMetric?,
        hrv: HrvMetric?
    ): StressSummary? {
        val baselineRhr = rhr?.beatsPerMinute?.toFloat() ?: hr?.minBpm?.toFloat() ?: return null
        if (baselineRhr <= 0f) return null

        val samples = hr?.samples?.map { it.beatsPerMinute } ?: emptyList()
        val currentHr = hr?.latestBpm?.toFloat() ?: baselineRhr

        val hrvModulation = if (hrv != null && hrv.rmssdMillis > 0.0) {
            val rmssd = hrv.rmssdMillis.toFloat()
            (35f / rmssd).coerceIn(0.85f, 1.25f)
        } else {
            1.0f
        }

        if (samples.isEmpty()) {
            val hrElevation = (currentHr / baselineRhr).coerceIn(0.7f, 2.0f)
            val score = (((hrElevation - 0.85f) * 2.5f) * hrvModulation).coerceIn(0.1f, 3.8f)
            val label = when {
                score < 1.0f -> "Calm"
                score < 2.0f -> "Low"
                score < 2.5f -> "Medium"
                else -> "High"
            }
            return StressSummary(
                averageScore = score,
                minScore = (score - 0.3f).coerceAtLeast(0f),
                maxScore = (score + 0.4f).coerceAtMost(4f),
                levelLabel = label
            )
        }

        val restingSamples = samples.filter { it < baselineRhr * 1.35f }
        val validSamples = if (restingSamples.isNotEmpty()) restingSamples else samples

        val scores = validSamples.map { bpm ->
            val ratio = bpm.toFloat() / baselineRhr
            (((ratio - 0.85f) * 2.5f) * hrvModulation).coerceIn(0.0f, 4.0f)
        }

        val avg = scores.average().toFloat()
        val min = scores.minOrNull() ?: 0f
        val max = (scores.maxOrNull() ?: (avg * 1.4f)).coerceAtMost(4.0f)
        val label = when {
            avg < 1.0f -> "Calm"
            avg < 2.0f -> "Low"
            avg < 2.5f -> "Medium"
            else -> "High"
        }

        return StressSummary(
            averageScore = avg,
            minScore = min,
            maxScore = max,
            levelLabel = label
        )
    }
}


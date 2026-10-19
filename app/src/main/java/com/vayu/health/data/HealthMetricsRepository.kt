package com.vayu.health.data

import com.vayu.health.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthDateRangeCalculator(
    private val clock: () -> Instant = { Instant.now() },
    private val zoneId: () -> ZoneId = { ZoneId.systemDefault() }
) {
    data class DateWindows(
        val now: Instant,
        val startOfToday: Instant,
        val sleepWindowStart: Instant,
        val sleepHistoryStart: Instant
    )

    fun calculateWindows(targetDate: LocalDate? = null): DateWindows {
        val now = clock()
        val zone = zoneId()
        val isCurrentDay = targetDate == null || targetDate == now.atZone(zone).toLocalDate()
        val date = targetDate ?: now.atZone(zone).toLocalDate()
        val startOfTargetDay = date.atStartOfDay(zone).toInstant()
        val endOfTargetDay = date.plusDays(1).atStartOfDay(zone).toInstant()
        val queryEnd = if (isCurrentDay) now else endOfTargetDay
        // Sleep that ends on `date` can start in the afternoon/evening of `date - 1`
        val sleepWindowStart = startOfTargetDay.minus(14, ChronoUnit.HOURS)
        val sleepHistoryStart = startOfTargetDay.minus(7, ChronoUnit.DAYS)
        return DateWindows(
            now = queryEnd,
            startOfToday = startOfTargetDay,
            sleepWindowStart = sleepWindowStart,
            sleepHistoryStart = sleepHistoryStart
        )
    }
}

class HealthMetricsRepository(
    private val healthConnectManager: HealthConnectManager,
    private val dateRangeCalculator: HealthDateRangeCalculator = HealthDateRangeCalculator(),
    private val googleHealthApiClient: com.vayu.health.data.network.GoogleHealthApiClient? = null,
    private val sourcePreferences: HealthSourcePreferences? = null
) {
    /**
     * Reads health metrics filtered by the specified DataOrigin source package and target date.
     * When sourceFilter is null, reads in mixed-source "All Health Connect Sources" mode.
     * When sourceFilter is non-null (e.g. "com.fitbit.FitbitMobile"), restricts all reads strictly to that origin.
     */
    suspend fun getTodayDashboardState(sourceFilter: String? = null, targetDate: LocalDate? = null): HealthDashboardState = withContext(Dispatchers.IO) {
        val sdkStatus = healthConnectManager.getSdkStatus()
        if (sdkStatus != HealthConnectStatus.AVAILABLE) {
            return@withContext HealthDashboardState(
                isLoading = false,
                sdkStatus = sdkStatus,
                selectedSource = sourceFilter,
                errorMessages = listOf(
                    if (sdkStatus == HealthConnectStatus.UPDATE_REQUIRED) {
                        "Health Connect needs to be updated from the Google Play Store."
                    } else {
                        "Health Connect is not available on this device."
                    }
                )
            )
        }

        val grantedPermissions = healthConnectManager.getGrantedPermissions()
        if (grantedPermissions.isEmpty()) {
            return@withContext HealthDashboardState(
                isLoading = false,
                sdkStatus = sdkStatus,
                selectedSource = sourceFilter,
                grantedPermissions = emptySet(),
                errorMessages = listOf("Health Connect read permissions not yet granted.")
            )
        }

        val effectiveDate = targetDate ?: LocalDate.now()
        val windows = dateRangeCalculator.calculateWindows(effectiveDate)
        val now = windows.now
        val startOfToday = windows.startOfToday
        val sleepWindowStart = windows.sleepWindowStart
        val historyStart = windows.sleepHistoryStart

        // Detect all active origins across records
        val detectedSources = healthConnectManager.detectAvailableDataOrigins(historyStart, now)
        val isFitbitDetected = detectedSources.any { it.contains("fitbit", ignoreCase = true) }
        val fitbitSource = detectedSources.firstOrNull { it.contains("fitbit", ignoreCase = true) }

        val errors = mutableListOf<String>()

        val sleep = try {
            healthConnectManager.readSleep(sleepWindowStart, now, sourceFilter, effectiveDate)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("sleep", e))
            null
        }

        val sleepHistory = try {
            healthConnectManager.readSleepHistory(historyStart, now, sourceFilter)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("sleep history", e))
            emptyList()
        }

        // Use a 30-day lookback for vitals to capture historical trends and recent nocturnal readings
        val vitalsHistoryStart = startOfToday.minus(30, ChronoUnit.DAYS)

        val restingHr = try {
            healthConnectManager.readRestingHeartRate(vitalsHistoryStart, now, sourceFilter, effectiveDate, sleep)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("resting heart rate", e))
            null
        }

        val restingHrHistory = try {
            healthConnectManager.readRestingHeartRateHistory(vitalsHistoryStart, now, sourceFilter)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("resting heart rate history", e))
            emptyList()
        }

        val hrv = try {
            healthConnectManager.readHrv(vitalsHistoryStart, now, sourceFilter, effectiveDate, sleep)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("HRV", e))
            null
        }

        val hrvHistory = try {
            healthConnectManager.readHrvHistory(vitalsHistoryStart, now, sourceFilter)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("HRV history", e))
            emptyList()
        }

        // Include samples from previous evening so nocturnal heart rate throughout the night is preserved
        val heartRate = try {
            healthConnectManager.readHeartRate(startOfToday.minus(8, ChronoUnit.HOURS), now, sourceFilter)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("heart rate", e))
            null
        }

        // Prioritize wearable (Fitbit) when sourceFilter is null to avoid double counting with Samsung Health phone accelerometer steps
        val activity = try {
            if (sourceFilter == null && fitbitSource != null) {
                val fitbitAct = healthConnectManager.readActivity(startOfToday, now, fitbitSource)
                fitbitAct ?: healthConnectManager.readActivity(startOfToday, now, null)
            } else {
                healthConnectManager.readActivity(startOfToday, now, sourceFilter)
            }
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("activity", e))
            null
        }

        val respiratory = try {
            healthConnectManager.readRespiratory(vitalsHistoryStart, now, sourceFilter, effectiveDate, sleep)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("respiratory", e))
            null
        }

        val rawRecords = healthConnectManager.readRawRecordsForInspector(sleepWindowStart, now, sourceFilter)
        val isSkinTempSupported = healthConnectManager.isSkinTemperatureSupported()

        var finalSleep = sleep
        var finalActivity = activity
        var isCloudActive = false

        // Cloud Synchronization via Google Health API (when configured)
        if (googleHealthApiClient != null && googleHealthApiClient.isConfigured()) {
            val target = targetDate ?: LocalDate.now()
            try {
                val cloudSleep = googleHealthApiClient.fetchSleepData(target)
                if (cloudSleep.isSuccessful && cloudSleep.data != null) {
                    val cs = cloudSleep.data
                    isCloudActive = true
                    finalSleep = if (finalSleep == null) {
                        cs
                    } else {
                        finalSleep.copy(
                            recordedScore = finalSleep.recordedScore ?: cs.recordedScore,
                            stages = if (finalSleep.stages.isEmpty() && cs.stages.isNotEmpty()) cs.stages else finalSleep.stages,
                            actualSleepDurationMinutes = finalSleep.actualSleepDurationMinutes ?: cs.actualSleepDurationMinutes
                        )
                    }
                }

                val cloudSteps = googleHealthApiClient.fetchStepsData(target)
                if (cloudSteps.isSuccessful && cloudSteps.data != null && cloudSteps.data > 0L) {
                    isCloudActive = true
                    val currentSteps = finalActivity?.steps ?: 0L
                    if (cloudSteps.data > currentSteps) {
                        finalActivity = finalActivity?.copy(steps = cloudSteps.data)
                            ?: ActivityMetric(
                                steps = cloudSteps.data,
                                activeCaloriesKcal = null,
                                totalCaloriesKcal = null,
                                distanceMeters = null,
                                sources = setOf(com.vayu.health.data.network.GoogleHealthApiClient.DATA_ORIGIN_CLOUD),
                                lastUpdated = now
                            )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("HealthMetricsRepository", "Cloud sync non-fatal error: ${e.message}")
            }
        }

        // Fallback for Resting Heart Rate if Health Connect has no RestingHeartRateRecord but pulse samples exist
        var finalRestingHr = restingHr
        if (finalRestingHr == null && heartRate != null && heartRate.samples.isNotEmpty()) {
            val sleepSamples = if (finalSleep != null) {
                heartRate.samples.filter { !it.time.isBefore(finalSleep.startTime) && !it.time.isAfter(finalSleep.endTime) }
            } else emptyList()

            val derivedBpm = if (sleepSamples.isNotEmpty()) {
                val sleepingAvg = sleepSamples.map { it.beatsPerMinute }.average()
                (sleepingAvg + 6.0).toLong().coerceIn(40L, 120L)
            } else {
                val sortedBpm = heartRate.samples.map { it.beatsPerMinute }.sorted()
                val p10Index = (sortedBpm.size * 0.10).toInt().coerceIn(0, sortedBpm.lastIndex)
                sortedBpm[p10Index].coerceIn(40L, 120L)
            }

            finalRestingHr = RestingHeartRateMetric(
                beatsPerMinute = derivedBpm,
                timestamp = heartRate.timestamp ?: now,
                sourcePackage = heartRate.sourcePackage
            )
        }

        val sleepQuality = HealthDataCalculations.calculateSleepQuality(
            sleep = finalSleep,
            nocturnalHeartRateSamples = heartRate?.samples ?: emptyList()
        )

        val resilience = HealthDataCalculations.calculateResilience(
            sleep = finalSleep,
            activity = finalActivity,
            hrv = hrv,
            rhr = finalRestingHr
        )

        val readinessResult = HealthDataCalculations.calculateReadiness(
            sleepHistory = sleepHistory,
            todaySleep = finalSleep,
            todayHrv = hrv,
            todayRhr = finalRestingHr
        )

        // Evaluate if Fitbit sync is needed (Fitbit source active but no records found)
        val isFitbitSource = sourceFilter != null && sourceFilter.contains("fitbit", ignoreCase = true)
        val hasAnyFitbitData = finalSleep != null || heartRate != null || finalRestingHr != null || hrv != null || finalActivity != null || respiratory != null
        val isFitbitSyncNeeded = isFitbitSource && !hasAnyFitbitData

        HealthDashboardState(
            isLoading = false,
            sdkStatus = sdkStatus,
            grantedPermissions = grantedPermissions,
            lastRefreshTime = now,
            selectedSource = sourceFilter,
            detectedSources = detectedSources,
            isFitbitDetected = isFitbitDetected,
            isFitbitSyncNeeded = isFitbitSyncNeeded,
            sleep = finalSleep,
            dailySleepHistory = sleepHistory,
            readinessScore = readinessResult.score,
            readinessLevel = readinessResult.levelLabel,
            restingHeartRate = finalRestingHr,
            dailyRestingHrHistory = restingHrHistory,
            hrv = hrv,
            dailyHrvHistory = hrvHistory,
            heartRate = heartRate,
            activity = finalActivity,
            respiratory = respiratory,
            resilience = resilience,
            sleepQuality = sleepQuality,
            rawRecords = rawRecords,
            isSkinTemperatureSupported = isSkinTempSupported,
            isCloudSyncActive = isCloudActive,
            errorMessages = errors
        )
    }

    suspend fun getLiveHeartRate(sourceFilter: String? = null): HeartRateMetric? = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val startOfToday = LocalDate.now().atStartOfDay(zone).toInstant()
        try {
            healthConnectManager.readHeartRate(startOfToday.minus(8, ChronoUnit.HOURS), now, sourceFilter)
        } catch (e: Exception) {
            null
        }
    }

    internal fun sanitizeErrorMessage(metricName: String, e: Exception): String {
        val raw = e.message?.takeIf { it.isNotBlank() } ?: "System read error"
        val clean = raw.replace(Regex("@[0-9a-fA-F]+"), "").trim()
        return "Error reading $metricName: $clean"
    }
}

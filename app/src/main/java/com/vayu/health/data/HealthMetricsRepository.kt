package com.vayu.health.data

import com.vayu.health.data.model.HealthConnectStatus
import com.vayu.health.data.model.HealthDashboardState
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

    fun calculateWindows(): DateWindows {
        val now = clock()
        val zone = zoneId()
        val startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val sleepWindowStart = now.minus(24, ChronoUnit.HOURS)
        val sleepHistoryStart = now.minus(7, ChronoUnit.DAYS)
        return DateWindows(
            now = now,
            startOfToday = startOfToday,
            sleepWindowStart = sleepWindowStart,
            sleepHistoryStart = sleepHistoryStart
        )
    }
}

class HealthMetricsRepository(
    private val healthConnectManager: HealthConnectManager,
    private val dateRangeCalculator: HealthDateRangeCalculator = HealthDateRangeCalculator()
) {
    /**
     * Reads health metrics filtered by the specified DataOrigin source package.
     * When sourceFilter is null, reads in mixed-source "All Health Connect Sources" mode.
     * When sourceFilter is non-null (e.g. "com.fitbit.FitbitMobile"), restricts all reads strictly to that origin.
     */
    suspend fun getTodayDashboardState(sourceFilter: String? = "com.fitbit.FitbitMobile"): HealthDashboardState = withContext(Dispatchers.IO) {
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

        val windows = dateRangeCalculator.calculateWindows()
        val now = windows.now
        val startOfToday = windows.startOfToday
        val sleepWindowStart = windows.sleepWindowStart
        val historyStart = windows.sleepHistoryStart

        // Detect all active origins across records
        val detectedSources = healthConnectManager.detectAvailableDataOrigins(historyStart, now)
        val isFitbitDetected = detectedSources.any { it.contains("fitbit", ignoreCase = true) }

        val errors = mutableListOf<String>()

        val sleep = try {
            healthConnectManager.readSleep(sleepWindowStart, now, sourceFilter)
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

        val restingHr = try {
            healthConnectManager.readRestingHeartRate(startOfToday, now, sourceFilter)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("resting heart rate", e))
            null
        }

        val hrv = try {
            healthConnectManager.readHrv(sleepWindowStart, now, sourceFilter)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("HRV", e))
            null
        }

        val heartRate = try {
            healthConnectManager.readHeartRate(startOfToday, now, sourceFilter)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("heart rate", e))
            null
        }

        val activity = try {
            healthConnectManager.readActivity(startOfToday, now, sourceFilter)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("activity", e))
            null
        }

        val respiratory = try {
            healthConnectManager.readRespiratory(sleepWindowStart, now, sourceFilter)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("respiratory", e))
            null
        }

        val rawRecords = healthConnectManager.readRawRecordsForInspector(sleepWindowStart, now, sourceFilter)
        val isSkinTempSupported = healthConnectManager.isSkinTemperatureSupported()

        // Evaluate if Fitbit sync is needed (Fitbit source active but no records found)
        val isFitbitSource = sourceFilter != null && sourceFilter.contains("fitbit", ignoreCase = true)
        val hasAnyFitbitData = sleep != null || heartRate != null || restingHr != null || hrv != null || activity != null || respiratory != null
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
            sleep = sleep,
            dailySleepHistory = sleepHistory,
            restingHeartRate = restingHr,
            hrv = hrv,
            heartRate = heartRate,
            activity = activity,
            respiratory = respiratory,
            rawRecords = rawRecords,
            isSkinTemperatureSupported = isSkinTempSupported,
            errorMessages = errors
        )
    }

    internal fun sanitizeErrorMessage(metricName: String, e: Exception): String {
        val raw = e.message?.takeIf { it.isNotBlank() } ?: "System read error"
        val clean = raw.replace(Regex("@[0-9a-fA-F]+"), "").trim()
        return "Error reading $metricName: $clean"
    }
}

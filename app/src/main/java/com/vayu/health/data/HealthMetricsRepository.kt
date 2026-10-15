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
    suspend fun getTodayDashboardState(): HealthDashboardState = withContext(Dispatchers.IO) {
        val sdkStatus = healthConnectManager.getSdkStatus()
        if (sdkStatus != HealthConnectStatus.AVAILABLE) {
            return@withContext HealthDashboardState(
                isLoading = false,
                sdkStatus = sdkStatus,
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
                grantedPermissions = emptySet(),
                errorMessages = listOf("Health Connect read permissions not yet granted.")
            )
        }

        val windows = dateRangeCalculator.calculateWindows()
        val now = windows.now
        val startOfToday = windows.startOfToday
        val sleepWindowStart = windows.sleepWindowStart
        val historyStart = windows.sleepHistoryStart

        val errors = mutableListOf<String>()

        val sleep = try {
            healthConnectManager.readSleep(sleepWindowStart, now)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("sleep", e))
            null
        }

        val sleepHistory = try {
            healthConnectManager.readSleepHistory(historyStart, now)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("sleep history", e))
            emptyList()
        }

        val restingHr = try {
            healthConnectManager.readRestingHeartRate(startOfToday, now)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("resting heart rate", e))
            null
        }

        val hrv = try {
            healthConnectManager.readHrv(sleepWindowStart, now)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("HRV", e))
            null
        }

        val heartRate = try {
            healthConnectManager.readHeartRate(startOfToday, now)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("heart rate", e))
            null
        }

        val activity = try {
            healthConnectManager.readActivity(startOfToday, now)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("activity", e))
            null
        }

        val respiratory = try {
            healthConnectManager.readRespiratory(sleepWindowStart, now)
        } catch (e: Exception) {
            errors.add(sanitizeErrorMessage("respiratory", e))
            null
        }

        val rawRecords = healthConnectManager.readRawRecordsForInspector(sleepWindowStart, now)
        val isSkinTempSupported = healthConnectManager.isSkinTemperatureSupported()

        HealthDashboardState(
            isLoading = false,
            sdkStatus = sdkStatus,
            grantedPermissions = grantedPermissions,
            lastRefreshTime = now,
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

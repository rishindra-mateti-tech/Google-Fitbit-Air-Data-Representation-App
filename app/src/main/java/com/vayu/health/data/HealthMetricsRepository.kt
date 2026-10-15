package com.vayu.health.data

import com.vayu.health.data.model.HealthDashboardState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthMetricsRepository(
    private val healthConnectManager: HealthConnectManager
) {
    suspend fun getTodayDashboardState(): HealthDashboardState = withContext(Dispatchers.IO) {
        if (!healthConnectManager.isAvailable()) {
            return@withContext HealthDashboardState(
                isHealthConnectAvailable = false,
                errorMessage = "Health Connect is not available on this device."
            )
        }

        val hasPermissions = healthConnectManager.hasAllPermissions()
        if (!hasPermissions) {
            return@withContext HealthDashboardState(
                hasPermissions = false,
                errorMessage = "Health Connect read permissions not yet granted."
            )
        }

        // Window: From midnight of today (or 24 hours ago for sleep) to right now
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        // For sleep, we look back up to 24 hours to catch last night's session
        val sleepWindowStart = now.minus(24, ChronoUnit.HOURS)

        try {
            val sleep = healthConnectManager.readSleep(sleepWindowStart, now)
            val restingHr = healthConnectManager.readRestingHeartRate(startOfToday, now)
            val hrv = healthConnectManager.readHrv(sleepWindowStart, now)
            val heartRate = healthConnectManager.readHeartRate(startOfToday, now)
            val activity = healthConnectManager.readActivity(startOfToday, now)
            val respiratory = healthConnectManager.readRespiratory(sleepWindowStart, now)
            val rawRecords = healthConnectManager.readRawRecordsForInspector(sleepWindowStart, now)

            HealthDashboardState(
                isLoading = false,
                isHealthConnectAvailable = true,
                hasPermissions = true,
                lastRefreshTime = now,
                sleep = sleep,
                restingHeartRate = restingHr,
                hrv = hrv,
                heartRate = heartRate,
                activity = activity,
                respiratory = respiratory,
                rawRecords = rawRecords
            )
        } catch (e: Exception) {
            HealthDashboardState(
                isLoading = false,
                hasPermissions = true,
                errorMessage = "Error reading Health Connect: ${e.localizedMessage}"
            )
        }
    }
}

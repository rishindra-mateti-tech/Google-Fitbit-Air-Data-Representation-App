package com.vayu.health.data

import com.vayu.health.data.model.HealthConnectStatus
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

        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        // For sleep, we look back up to 24 hours to catch last night's session
        val sleepWindowStart = now.minus(24, ChronoUnit.HOURS)
        // For 7-day sleep history, look back 7 days
        val historyStart = now.minus(7, ChronoUnit.DAYS)

        val errors = mutableListOf<String>()

        val sleep = try {
            healthConnectManager.readSleep(sleepWindowStart, now)
        } catch (e: Exception) {
            errors.add("Error reading sleep: ${e.message}")
            null
        }

        val sleepHistory = try {
            healthConnectManager.readSleepHistory(historyStart, now)
        } catch (e: Exception) {
            errors.add("Error reading sleep history: ${e.message}")
            emptyList()
        }

        val restingHr = try {
            healthConnectManager.readRestingHeartRate(startOfToday, now)
        } catch (e: Exception) {
            errors.add("Error reading resting heart rate: ${e.message}")
            null
        }

        val hrv = try {
            healthConnectManager.readHrv(sleepWindowStart, now)
        } catch (e: Exception) {
            errors.add("Error reading HRV: ${e.message}")
            null
        }

        val heartRate = try {
            healthConnectManager.readHeartRate(startOfToday, now)
        } catch (e: Exception) {
            errors.add("Error reading heart rate: ${e.message}")
            null
        }

        val activity = try {
            healthConnectManager.readActivity(startOfToday, now)
        } catch (e: Exception) {
            errors.add("Error reading activity: ${e.message}")
            null
        }

        val respiratory = try {
            healthConnectManager.readRespiratory(sleepWindowStart, now)
        } catch (e: Exception) {
            errors.add("Error reading respiratory: ${e.message}")
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
}

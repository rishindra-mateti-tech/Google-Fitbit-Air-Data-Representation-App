package com.vayu.health.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.vayu.health.data.model.*
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    val healthConnectClient by lazy {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            null
        }
    }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(SkinTemperatureRecord::class)
    )

    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return permissions.all { it in granted }
    }

    suspend fun readSleep(start: Instant, end: Instant): SleepMetric? {
        val client = healthConnectClient ?: return null
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val latestSession = response.records.maxByOrNull { it.endTime } ?: return null
            
            var deepMins = 0L
            var remMins = 0L
            var lightMins = 0L
            var awakeMins = 0L
            val segments = mutableListOf<SleepStageSegment>()

            latestSession.stages.forEach { stage ->
                val duration = Duration.between(stage.startTime, stage.endTime).toMinutes().coerceAtLeast(0)
                val stageType = when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_DEEP -> {
                        deepMins += duration
                        SleepStageType.DEEP
                    }
                    SleepSessionRecord.STAGE_TYPE_REM -> {
                        remMins += duration
                        SleepStageType.REM
                    }
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> {
                        lightMins += duration
                        SleepStageType.LIGHT
                    }
                    SleepSessionRecord.STAGE_TYPE_AWAKE,
                    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> {
                        awakeMins += duration
                        SleepStageType.AWAKE
                    }
                    else -> SleepStageType.UNKNOWN
                }
                segments.add(
                    SleepStageSegment(
                        stage = stageType,
                        startTime = stage.startTime,
                        endTime = stage.endTime,
                        durationMinutes = duration
                    )
                )
            }

            val totalDuration = Duration.between(latestSession.startTime, latestSession.endTime).toMinutes()

            SleepMetric(
                totalDurationMinutes = totalDuration,
                deepMinutes = deepMins,
                remMinutes = remMins,
                lightMinutes = lightMins,
                awakeMinutes = awakeMins,
                startTime = latestSession.startTime,
                endTime = latestSession.endTime,
                stages = segments,
                sourcePackage = latestSession.metadata.dataOrigin.packageName
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun readRestingHeartRate(start: Instant, end: Instant): RestingHeartRateMetric? {
        val client = healthConnectClient ?: return null
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = RestingHeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val latest = response.records.maxByOrNull { it.time } ?: return null
            RestingHeartRateMetric(
                beatsPerMinute = latest.beatsPerMinute,
                timestamp = latest.time,
                sourcePackage = latest.metadata.dataOrigin.packageName
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun readHrv(start: Instant, end: Instant): HrvMetric? {
        val client = healthConnectClient ?: return null
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val latest = response.records.maxByOrNull { it.time } ?: return null
            HrvMetric(
                rmssdMillis = latest.heartRateVariabilityMillis,
                timestamp = latest.time,
                sourcePackage = latest.metadata.dataOrigin.packageName
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun readHeartRate(start: Instant, end: Instant): HeartRateMetric? {
        val client = healthConnectClient ?: return null
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val allSamples = response.records.flatMap { it.samples }
            if (allSamples.isEmpty()) return null

            val minBpm = allSamples.minOfOrNull { it.beatsPerMinute }
            val maxBpm = allSamples.maxOfOrNull { it.beatsPerMinute }
            val latestSample = allSamples.maxByOrNull { it.time }
            val latestRecordOrigin = response.records.lastOrNull()?.metadata?.dataOrigin?.packageName ?: "Unknown"

            HeartRateMetric(
                latestBpm = latestSample?.beatsPerMinute,
                minBpm = minBpm,
                maxBpm = maxBpm,
                samplesCount = allSamples.size,
                timestamp = latestSample?.time,
                sourcePackage = latestRecordOrigin
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun readActivity(start: Instant, end: Instant): ActivityMetric? {
        val client = healthConnectClient ?: return null
        return try {
            val stepsResp = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val totalSteps = if (stepsResp.records.isNotEmpty()) stepsResp.records.sumOf { it.count } else null

            val activeCalResp = client.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val totalActiveCal = if (activeCalResp.records.isNotEmpty()) {
                activeCalResp.records.sumOf { it.energy.inKilocalories }
            } else null

            val totalCalResp = client.readRecords(
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val totalCal = if (totalCalResp.records.isNotEmpty()) {
                totalCalResp.records.sumOf { it.energy.inKilocalories }
            } else null

            val distResp = client.readRecords(
                ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val totalDist = if (distResp.records.isNotEmpty()) {
                distResp.records.sumOf { it.distance.inMeters }
            } else null

            val lastUpdate = stepsResp.records.maxByOrNull { it.endTime }?.endTime

            if (totalSteps == null && totalActiveCal == null && totalCal == null) null
            else ActivityMetric(
                steps = totalSteps,
                activeCaloriesKcal = totalActiveCal,
                totalCaloriesKcal = totalCal,
                distanceMeters = totalDist,
                lastUpdated = lastUpdate
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun readRespiratory(start: Instant, end: Instant): RespiratoryMetric? {
        val client = healthConnectClient ?: return null
        return try {
            val respRateResp = client.readRecords(
                ReadRecordsRequest(
                    recordType = RespiratoryRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val latestResp = respRateResp.records.maxByOrNull { it.time }

            val spo2Resp = client.readRecords(
                ReadRecordsRequest(
                    recordType = OxygenSaturationRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val latestSpo2 = spo2Resp.records.maxByOrNull { it.time }

            val skinTempResp = client.readRecords(
                ReadRecordsRequest(
                    recordType = SkinTemperatureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val latestSkinTemp = skinTempResp.records.maxByOrNull { it.startTime }
            val skinTempDelta = latestSkinTemp?.deltas?.firstOrNull()?.delta?.inCelsius

            if (latestResp == null && latestSpo2 == null && skinTempDelta == null) null
            else RespiratoryMetric(
                breathsPerMinute = latestResp?.rate,
                spo2Percentage = latestSpo2?.percentage?.value,
                skinTempDeviationCelsius = skinTempDelta,
                timestamp = latestResp?.time ?: latestSpo2?.time ?: latestSkinTemp?.startTime
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun readRawRecordsForInspector(start: Instant, end: Instant): List<RawHealthRecord> {
        val client = healthConnectClient ?: return emptyList()
        val list = mutableListOf<RawHealthRecord>()

        try {
            val sleep = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(start, end)))
            sleep.records.forEach {
                val dur = Duration.between(it.startTime, it.endTime).toMinutes()
                list.add(
                    RawHealthRecord(
                        recordType = "SleepSessionRecord",
                        valueSummary = "Duration: ${dur / 60}h ${dur % 60}m (${it.stages.size} stages)",
                        startTime = it.startTime,
                        endTime = it.endTime,
                        clientPackage = it.metadata.dataOrigin.packageName,
                        recordId = it.metadata.id
                    )
                )
            }

            val hrv = client.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, TimeRangeFilter.between(start, end)))
            hrv.records.forEach {
                list.add(
                    RawHealthRecord(
                        recordType = "HeartRateVariabilityRmssdRecord",
                        valueSummary = "HRV: ${"%.1f".format(it.heartRateVariabilityMillis)} ms",
                        startTime = it.time,
                        endTime = null,
                        clientPackage = it.metadata.dataOrigin.packageName,
                        recordId = it.metadata.id
                    )
                )
            }

            val rhr = client.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, TimeRangeFilter.between(start, end)))
            rhr.records.forEach {
                list.add(
                    RawHealthRecord(
                        recordType = "RestingHeartRateRecord",
                        valueSummary = "Resting HR: ${it.beatsPerMinute} bpm",
                        startTime = it.time,
                        endTime = null,
                        clientPackage = it.metadata.dataOrigin.packageName,
                        recordId = it.metadata.id
                    )
                )
            }

            val steps = client.readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, end)))
            steps.records.forEach {
                list.add(
                    RawHealthRecord(
                        recordType = "StepsRecord",
                        valueSummary = "Steps: ${it.count}",
                        startTime = it.startTime,
                        endTime = it.endTime,
                        clientPackage = it.metadata.dataOrigin.packageName,
                        recordId = it.metadata.id
                    )
                )
            }

            val cal = client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end)))
            cal.records.forEach {
                list.add(
                    RawHealthRecord(
                        recordType = "ActiveCaloriesBurnedRecord",
                        valueSummary = "Active: ${"%.0f".format(it.energy.inKilocalories)} kcal",
                        startTime = it.startTime,
                        endTime = it.endTime,
                        clientPackage = it.metadata.dataOrigin.packageName,
                        recordId = it.metadata.id
                    )
                )
            }
        } catch (e: Exception) {
            // Return records captured so far
        }

        return list.sortedByDescending { it.startTime }
    }
}

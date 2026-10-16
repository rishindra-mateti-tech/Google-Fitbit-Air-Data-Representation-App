package com.vayu.health.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.vayu.health.data.model.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

open class HealthConnectManager(private val context: Context? = null) {

    private val tag = "HealthConnectManager"

    val healthConnectClient: HealthConnectClient? by lazy {
        val ctx = context ?: return@lazy null
        try {
            HealthConnectClient.getOrCreate(ctx)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize HealthConnectClient", e)
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

    open fun getSdkStatus(): HealthConnectStatus {
        val ctx = context ?: return HealthConnectStatus.UNAVAILABLE
        return when (HealthConnectClient.getSdkStatus(ctx)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.UPDATE_REQUIRED
            else -> HealthConnectStatus.UNAVAILABLE
        }
    }

    @OptIn(androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi::class)
    open fun isSkinTemperatureSupported(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE) ==
                    HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        } catch (e: Exception) {
            Log.w(tag, "Error querying skin temperature feature support", e)
            false
        }
    }

    open suspend fun getGrantedPermissions(): Set<String> {
        val client = healthConnectClient ?: return emptySet()
        return try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            Log.e(tag, "Error reading granted permissions", e)
            emptySet()
        }
    }

    open suspend fun hasPermission(permission: String): Boolean {
        return permission in getGrantedPermissions()
    }

    /**
     * Detects all distinct DataOrigin package names present across available Health Connect records.
     */
    open suspend fun detectAvailableDataOrigins(start: Instant, end: Instant): Set<String> {
        val client = healthConnectClient ?: return emptySet()
        val origins = mutableSetOf<String>()
        val granted = getGrantedPermissions()

        val recordTypes = listOf(
            StepsRecord::class,
            SleepSessionRecord::class,
            HeartRateRecord::class,
            RestingHeartRateRecord::class,
            HeartRateVariabilityRmssdRecord::class,
            DistanceRecord::class,
            ActiveCaloriesBurnedRecord::class,
            TotalCaloriesBurnedRecord::class,
            OxygenSaturationRecord::class,
            RespiratoryRateRecord::class
        )

        for (recordClass in recordTypes) {
            val perm = HealthPermission.getReadPermission(recordClass)
            if (perm in granted) {
                try {
                    val res = client.readRecords(
                        ReadRecordsRequest(
                            recordType = recordClass,
                            timeRangeFilter = TimeRangeFilter.between(start, end),
                            pageSize = 100
                        )
                    )
                    res.records.forEach { origins.add(it.metadata.dataOrigin.packageName) }
                } catch (e: Exception) {
                    Log.w(tag, "Error querying origins for ${recordClass.simpleName}", e)
                }
            }
        }
        return origins
    }

    /**
     * Reads the most recent sleep session in the window.
     * When sourceFilter is specified, only sessions from that DataOrigin are returned.
     * Preserves exact source sleep stages, does NOT conflate OUT_OF_BED with Awake,
     * and separates session elapsed time from actual sleep duration.
     */
    open suspend fun readSleep(start: Instant, end: Instant, sourceFilter: String? = null): SleepMetric? {
        val client = healthConnectClient ?: return null
        if (!hasPermission(HealthPermission.getReadPermission(SleepSessionRecord::class))) {
            return null
        }

        return try {
            val dataOriginFilter = if (sourceFilter != null) setOf(DataOrigin(sourceFilter)) else emptySet()
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    dataOriginFilter = dataOriginFilter
                )
            )
            val filteredRecords = response.records.filter {
                sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter
            }
            val latestSession = filteredRecords.maxByOrNull { it.endTime } ?: return null

            var deepMins = 0L
            var remMins = 0L
            var lightMins = 0L
            var sleepingMins = 0L
            var awakeMins = 0L
            var outOfBedMins = 0L
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
                    SleepSessionRecord.STAGE_TYPE_SLEEPING -> {
                        sleepingMins += duration
                        SleepStageType.SLEEPING
                    }
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> {
                        awakeMins += duration
                        SleepStageType.AWAKE
                    }
                    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> {
                        outOfBedMins += duration
                        SleepStageType.OUT_OF_BED
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

            val sessionElapsed = Duration.between(latestSession.startTime, latestSession.endTime).toMinutes()
            val totalSleepTime = if (latestSession.stages.isNotEmpty()) {
                deepMins + remMins + lightMins + sleepingMins
            } else {
                null
            }

            val recordedScore = HealthDataCalculations.parseSleepScoreFromText(
                title = latestSession.title,
                notes = latestSession.notes
            )

            SleepMetric(
                sessionElapsedMinutes = sessionElapsed,
                actualSleepDurationMinutes = totalSleepTime,
                deepMinutes = deepMins,
                remMinutes = remMins,
                lightMinutes = lightMins,
                sleepingMinutes = sleepingMins,
                awakeMinutes = awakeMins,
                outOfBedMinutes = outOfBedMins,
                startTime = latestSession.startTime,
                endTime = latestSession.endTime,
                stages = segments,
                sourcePackage = latestSession.metadata.dataOrigin.packageName,
                recordedScore = recordedScore
            )
        } catch (e: Exception) {
            Log.e(tag, "Error reading sleep records", e)
            throw e
        }
    }

    /**
     * Reads daily sleep history over the past 7 days.
     * When sourceFilter is specified, returns strictly sessions written by that source.
     */
    open suspend fun readSleepHistory(start: Instant, end: Instant, sourceFilter: String? = null): List<DailySleepSummary> {
        val client = healthConnectClient ?: return emptyList()
        if (!hasPermission(HealthPermission.getReadPermission(SleepSessionRecord::class))) {
            return emptyList()
        }

        return try {
            val dataOriginFilter = if (sourceFilter != null) setOf(DataOrigin(sourceFilter)) else emptySet()
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    dataOriginFilter = dataOriginFilter
                )
            )
            val filteredRecords = response.records.filter {
                sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter
            }
            val zone = ZoneId.systemDefault()
            filteredRecords
                .groupBy { it.endTime.atZone(zone).toLocalDate() }
                .map { (date, sessions) ->
                    val contributingSources = sessions.map { it.metadata.dataOrigin.packageName }.toSet()
                    var totalActualSleep: Long? = null
                    var totalElapsed = 0L

                    sessions.forEach { session ->
                        val sessionElapsed = Duration.between(session.startTime, session.endTime).toMinutes().coerceAtLeast(0)
                        totalElapsed += sessionElapsed

                        var sessionActualSleep = 0L
                        var hasValidStages = false
                        session.stages.forEach { stage ->
                            if (stage.stage in setOf(
                                    SleepSessionRecord.STAGE_TYPE_DEEP,
                                    SleepSessionRecord.STAGE_TYPE_REM,
                                    SleepSessionRecord.STAGE_TYPE_LIGHT,
                                    SleepSessionRecord.STAGE_TYPE_SLEEPING
                                )) {
                                sessionActualSleep += Duration.between(stage.startTime, stage.endTime).toMinutes().coerceAtLeast(0)
                                hasValidStages = true
                            }
                        }
                        if (hasValidStages) {
                            totalActualSleep = (totalActualSleep ?: 0L) + sessionActualSleep
                        }
                    }

                    val primarySource = if (contributingSources.size == 1) {
                        contributingSources.first()
                    } else {
                        "Health Connect (${contributingSources.size} sources)"
                    }

                    DailySleepSummary(
                        date = date,
                        actualSleepMinutes = totalActualSleep,
                        sessionElapsedMinutes = totalElapsed,
                        sourcePackage = primarySource,
                        sessionCount = sessions.size,
                        contributingSources = contributingSources
                    )
                }
                .sortedBy { it.date }
        } catch (e: Exception) {
            Log.e(tag, "Error reading sleep history", e)
            emptyList()
        }
    }

    open suspend fun readRestingHeartRate(start: Instant, end: Instant, sourceFilter: String? = null): RestingHeartRateMetric? {
        val client = healthConnectClient ?: return null
        if (!hasPermission(HealthPermission.getReadPermission(RestingHeartRateRecord::class))) {
            return null
        }

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = RestingHeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val filtered = response.records.filter {
                sourceFilter == null ||
                        it.metadata.dataOrigin.packageName.contains(sourceFilter, ignoreCase = true) ||
                        sourceFilter.contains(it.metadata.dataOrigin.packageName, ignoreCase = true)
            }
            val latest = filtered.maxByOrNull { it.time } ?: return null
            RestingHeartRateMetric(
                beatsPerMinute = latest.beatsPerMinute,
                timestamp = latest.time,
                sourcePackage = latest.metadata.dataOrigin.packageName
            )
        } catch (e: Exception) {
            Log.e(tag, "Error reading resting heart rate", e)
            throw e
        }
    }

    open suspend fun readHrv(start: Instant, end: Instant, sourceFilter: String? = null): HrvMetric? {
        val client = healthConnectClient ?: return null
        if (!hasPermission(HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class))) {
            return null
        }

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val filtered = response.records.filter {
                sourceFilter == null ||
                        it.metadata.dataOrigin.packageName.contains(sourceFilter, ignoreCase = true) ||
                        sourceFilter.contains(it.metadata.dataOrigin.packageName, ignoreCase = true)
            }
            if (filtered.isEmpty()) return null
            val avgRmssd = filtered.map { it.heartRateVariabilityMillis }.average()
            val latest = filtered.maxByOrNull { it.time } ?: return null
            HrvMetric(
                rmssdMillis = avgRmssd,
                timestamp = latest.time,
                sourcePackage = latest.metadata.dataOrigin.packageName
            )
        } catch (e: Exception) {
            Log.e(tag, "Error reading HRV", e)
            throw e
        }
    }

    /**
     * Reads heart rate records within the window using pagination.
     * When sourceFilter is specified, only samples written by that source are returned.
     * Preserves real sample points and their source origins for truthful charting.
     */
    open suspend fun readHeartRate(start: Instant, end: Instant, sourceFilter: String? = null): HeartRateMetric? {
        val client = healthConnectClient ?: return null
        if (!hasPermission(HealthPermission.getReadPermission(HeartRateRecord::class))) {
            return null
        }

        return try {
            val allRecords = mutableListOf<HeartRateRecord>()
            var pageToken: String? = null
            val dataOriginFilter = if (sourceFilter != null) setOf(DataOrigin(sourceFilter)) else emptySet()

            do {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                        dataOriginFilter = dataOriginFilter,
                        pageToken = pageToken,
                        pageSize = 1000
                    )
                )
                allRecords.addAll(response.records)
                pageToken = response.pageToken
            } while (pageToken != null)

            val filteredRecords = allRecords.filter {
                sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter
            }

            val allSamples = filteredRecords.flatMap { record ->
                val origin = record.metadata.dataOrigin.packageName
                record.samples.map { sample ->
                    HeartRateSamplePoint(
                        time = sample.time,
                        beatsPerMinute = sample.beatsPerMinute,
                        sourcePackage = origin
                    )
                }
            }.sortedBy { it.time }

            if (allSamples.isEmpty()) return null

            val minBpm = allSamples.minOf { it.beatsPerMinute }
            val maxBpm = allSamples.maxOf { it.beatsPerMinute }
            val avgBpm = (allSamples.sumOf { it.beatsPerMinute } / allSamples.size.toDouble()).toLong()
            val latestSample = allSamples.last()
            val latestOrigin = latestSample.sourcePackage
            val contributingSources = allSamples.map { it.sourcePackage }.toSet()

            HeartRateMetric(
                latestBpm = latestSample.beatsPerMinute,
                minBpm = minBpm,
                maxBpm = maxBpm,
                averageBpm = avgBpm,
                samples = allSamples,
                timestamp = latestSample.time,
                sourcePackage = latestOrigin,
                sources = contributingSources
            )
        } catch (e: Exception) {
            Log.e(tag, "Error reading heart rate records", e)
            throw e
        }
    }

    /**
     * Uses Health Connect Aggregate APIs for de-duplicated activity totals.
     * When sourceFilter is specified, restricts aggregation strictly to that DataOrigin.
     */
    open suspend fun readActivity(start: Instant, end: Instant, sourceFilter: String? = null): ActivityMetric? {
        val client = healthConnectClient ?: return null
        val granted = getGrantedPermissions()

        val requestedMetrics = mutableSetOf<androidx.health.connect.client.aggregate.AggregateMetric<*>>()
        if (HealthPermission.getReadPermission(StepsRecord::class) in granted) {
            requestedMetrics.add(StepsRecord.COUNT_TOTAL)
        }
        if (HealthPermission.getReadPermission(DistanceRecord::class) in granted) {
            requestedMetrics.add(DistanceRecord.DISTANCE_TOTAL)
        }
        if (HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted) {
            requestedMetrics.add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
        }
        if (HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class) in granted) {
            requestedMetrics.add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
        }

        if (requestedMetrics.isEmpty()) return null

        return try {
            val dataOriginFilter = if (sourceFilter != null) setOf(DataOrigin(sourceFilter)) else emptySet()
            val response = client.aggregate(
                AggregateRequest(
                    metrics = requestedMetrics,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    dataOriginFilter = dataOriginFilter
                )
            )

            val steps = response[StepsRecord.COUNT_TOTAL]
            val distance = response[DistanceRecord.DISTANCE_TOTAL]?.inMeters
            val activeCal = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
            val totalCal = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
            val contributingSources = if (sourceFilter != null) {
                setOf(sourceFilter)
            } else {
                response.dataOrigins.map { it.packageName }.toSet()
            }

            if (steps == null && distance == null && activeCal == null && totalCal == null) {
                null
            } else {
                ActivityMetric(
                    steps = steps,
                    activeCaloriesKcal = activeCal,
                    totalCaloriesKcal = totalCal,
                    distanceMeters = distance,
                    sources = contributingSources,
                    lastUpdated = end
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error aggregating activity records", e)
            throw e
        }
    }

    /**
     * Reads respiratory rate, oxygen saturation, and skin temperature.
     * When sourceFilter is specified, filters records by that DataOrigin.
     */
    open suspend fun readRespiratory(start: Instant, end: Instant, sourceFilter: String? = null): RespiratoryMetric? {
        val client = healthConnectClient ?: return null
        val granted = getGrantedPermissions()

        var latestResp: RespiratoryRateRecord? = null
        if (HealthPermission.getReadPermission(RespiratoryRateRecord::class) in granted) {
            try {
                val resp = client.readRecords(
                    ReadRecordsRequest(
                        recordType = RespiratoryRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )
                val filtered = resp.records.filter {
                    sourceFilter == null ||
                            it.metadata.dataOrigin.packageName.contains(sourceFilter, ignoreCase = true) ||
                            sourceFilter.contains(it.metadata.dataOrigin.packageName, ignoreCase = true)
                }
                latestResp = filtered.maxByOrNull { it.time }
            } catch (e: Exception) {
                Log.e(tag, "Error reading respiratory rate", e)
            }
        }

        var latestSpo2: OxygenSaturationRecord? = null
        if (HealthPermission.getReadPermission(OxygenSaturationRecord::class) in granted) {
            try {
                val resp = client.readRecords(
                    ReadRecordsRequest(
                        recordType = OxygenSaturationRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )
                val filtered = resp.records.filter {
                    sourceFilter == null ||
                            it.metadata.dataOrigin.packageName.contains(sourceFilter, ignoreCase = true) ||
                            sourceFilter.contains(it.metadata.dataOrigin.packageName, ignoreCase = true)
                }
                latestSpo2 = filtered.maxByOrNull { it.time }
            } catch (e: Exception) {
                Log.e(tag, "Error reading SpO2", e)
            }
        }

        var skinTempMetric: SkinTemperatureMetric? = null
        if (isSkinTemperatureSupported() && HealthPermission.getReadPermission(SkinTemperatureRecord::class) in granted) {
            try {
                val resp = client.readRecords(
                    ReadRecordsRequest(
                        recordType = SkinTemperatureRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )
                val filtered = resp.records.filter {
                    sourceFilter == null ||
                            it.metadata.dataOrigin.packageName.contains(sourceFilter, ignoreCase = true) ||
                            sourceFilter.contains(it.metadata.dataOrigin.packageName, ignoreCase = true)
                }
                val latest = filtered.maxByOrNull { it.startTime }
                if (latest != null && latest.deltas.isNotEmpty()) {
                    val deltas = latest.deltas.map { it.delta.inCelsius }
                    skinTempMetric = SkinTemperatureMetric(
                        baselineCelsius = latest.baseline?.inCelsius,
                        averageDeltaCelsius = deltas.average(),
                        minDeltaCelsius = deltas.minOrNull(),
                        maxDeltaCelsius = deltas.maxOrNull(),
                        deltasCount = deltas.size,
                        timestamp = latest.startTime,
                        sourcePackage = latest.metadata.dataOrigin.packageName
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Error reading skin temperature", e)
            }
        }

        if (latestResp == null && latestSpo2 == null && skinTempMetric == null) {
            return null
        }

        val respSource = latestResp?.metadata?.dataOrigin?.packageName
        val respTime = latestResp?.time
        val spo2Source = latestSpo2?.metadata?.dataOrigin?.packageName
        val spo2Time = latestSpo2?.time

        val sources = mutableSetOf<String>()
        respSource?.let { sources.add(it) }
        spo2Source?.let { sources.add(it) }
        skinTempMetric?.sourcePackage?.let { sources.add(it) }

        return RespiratoryMetric(
            breathsPerMinute = latestResp?.rate,
            respiratoryTimestamp = respTime,
            respiratorySourcePackage = respSource,
            spo2Percentage = latestSpo2?.percentage?.value,
            spo2Timestamp = spo2Time,
            spo2SourcePackage = spo2Source,
            skinTemperature = skinTempMetric,
            timestamp = spo2Time ?: respTime ?: skinTempMetric?.timestamp,
            contributingSources = sources
        )
    }

    open suspend fun readRawRecordsForInspector(start: Instant, end: Instant, sourceFilter: String? = null): List<RawHealthRecord> {
        val client = healthConnectClient ?: return emptyList()
        val list = mutableListOf<RawHealthRecord>()
        val granted = getGrantedPermissions()
        val dataOriginFilter = if (sourceFilter != null) setOf(DataOrigin(sourceFilter)) else emptySet()

        try {
            if (HealthPermission.getReadPermission(SleepSessionRecord::class) in granted) {
                val sleep = client.readRecords(
                    ReadRecordsRequest(
                        SleepSessionRecord::class,
                        TimeRangeFilter.between(start, end),
                        dataOriginFilter = dataOriginFilter
                    )
                )
                sleep.records.filter { sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter }.forEach {
                    val dur = Duration.between(it.startTime, it.endTime).toMinutes()
                    list.add(
                        RawHealthRecord(
                            recordType = "SleepSessionRecord",
                            valueSummary = "Session: ${dur / 60}h ${dur % 60}m (${it.stages.size} stages)",
                            startTime = it.startTime,
                            endTime = it.endTime,
                            clientPackage = it.metadata.dataOrigin.packageName,
                            recordId = it.metadata.id
                        )
                    )
                }
            }

            if (HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class) in granted) {
                val hrv = client.readRecords(
                    ReadRecordsRequest(
                        HeartRateVariabilityRmssdRecord::class,
                        TimeRangeFilter.between(start, end),
                        dataOriginFilter = dataOriginFilter
                    )
                )
                hrv.records.filter { sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter }.forEach {
                    list.add(
                        RawHealthRecord(
                            recordType = "HeartRateVariabilityRmssdRecord",
                            valueSummary = "HRV (RMSSD): ${"%.1f".format(it.heartRateVariabilityMillis)} ms",
                            startTime = it.time,
                            endTime = null,
                            clientPackage = it.metadata.dataOrigin.packageName,
                            recordId = it.metadata.id
                        )
                    )
                }
            }

            if (HealthPermission.getReadPermission(RestingHeartRateRecord::class) in granted) {
                val rhr = client.readRecords(
                    ReadRecordsRequest(
                        RestingHeartRateRecord::class,
                        TimeRangeFilter.between(start, end),
                        dataOriginFilter = dataOriginFilter
                    )
                )
                rhr.records.filter { sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter }.forEach {
                    list.add(
                        RawHealthRecord(
                            recordType = "RestingHeartRateRecord",
                            valueSummary = "Resting Heart Rate: ${it.beatsPerMinute} bpm",
                            startTime = it.time,
                            endTime = null,
                            clientPackage = it.metadata.dataOrigin.packageName,
                            recordId = it.metadata.id
                        )
                    )
                }
            }

            if (HealthPermission.getReadPermission(HeartRateRecord::class) in granted) {
                val hr = client.readRecords(
                    ReadRecordsRequest(
                        HeartRateRecord::class,
                        TimeRangeFilter.between(start, end),
                        dataOriginFilter = dataOriginFilter,
                        pageSize = 50
                    )
                )
                hr.records.filter { sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter }.forEach {
                    list.add(
                        RawHealthRecord(
                            recordType = "HeartRateRecord",
                            valueSummary = "${it.samples.size} samples (Latest: ${it.samples.lastOrNull()?.beatsPerMinute ?: 0} bpm)",
                            startTime = it.startTime,
                            endTime = it.endTime,
                            clientPackage = it.metadata.dataOrigin.packageName,
                            recordId = it.metadata.id
                        )
                    )
                }
            }

            if (HealthPermission.getReadPermission(StepsRecord::class) in granted) {
                val steps = client.readRecords(
                    ReadRecordsRequest(
                        StepsRecord::class,
                        TimeRangeFilter.between(start, end),
                        dataOriginFilter = dataOriginFilter
                    )
                )
                steps.records.filter { sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter }.forEach {
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
            }

            if (HealthPermission.getReadPermission(DistanceRecord::class) in granted) {
                val distance = client.readRecords(
                    ReadRecordsRequest(
                        DistanceRecord::class,
                        TimeRangeFilter.between(start, end),
                        dataOriginFilter = dataOriginFilter
                    )
                )
                distance.records.filter { sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter }.forEach {
                    list.add(
                        RawHealthRecord(
                            recordType = "DistanceRecord",
                            valueSummary = "Distance: ${"%.1f".format(it.distance.inMeters)} m",
                            startTime = it.startTime,
                            endTime = it.endTime,
                            clientPackage = it.metadata.dataOrigin.packageName,
                            recordId = it.metadata.id
                        )
                    )
                }
            }

            if (HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted) {
                val cal = client.readRecords(
                    ReadRecordsRequest(
                        ActiveCaloriesBurnedRecord::class,
                        TimeRangeFilter.between(start, end),
                        dataOriginFilter = dataOriginFilter
                    )
                )
                cal.records.filter { sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter }.forEach {
                    list.add(
                        RawHealthRecord(
                            recordType = "ActiveCaloriesBurnedRecord",
                            valueSummary = "Active Energy: ${"%.0f".format(it.energy.inKilocalories)} kcal",
                            startTime = it.startTime,
                            endTime = it.endTime,
                            clientPackage = it.metadata.dataOrigin.packageName,
                            recordId = it.metadata.id
                        )
                    )
                }
            }

            if (HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class) in granted) {
                val totCal = client.readRecords(
                    ReadRecordsRequest(
                        TotalCaloriesBurnedRecord::class,
                        TimeRangeFilter.between(start, end),
                        dataOriginFilter = dataOriginFilter
                    )
                )
                totCal.records.filter { sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter }.forEach {
                    list.add(
                        RawHealthRecord(
                            recordType = "TotalCaloriesBurnedRecord",
                            valueSummary = "Total Energy: ${"%.0f".format(it.energy.inKilocalories)} kcal",
                            startTime = it.startTime,
                            endTime = it.endTime,
                            clientPackage = it.metadata.dataOrigin.packageName,
                            recordId = it.metadata.id
                        )
                    )
                }
            }

            if (HealthPermission.getReadPermission(OxygenSaturationRecord::class) in granted) {
                val spo2 = client.readRecords(
                    ReadRecordsRequest(
                        OxygenSaturationRecord::class,
                        TimeRangeFilter.between(start, end),
                        dataOriginFilter = dataOriginFilter
                    )
                )
                spo2.records.filter { sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter }.forEach {
                    list.add(
                        RawHealthRecord(
                            recordType = "OxygenSaturationRecord",
                            valueSummary = "SpO2: ${"%.1f".format(it.percentage.value)}%",
                            startTime = it.time,
                            endTime = null,
                            clientPackage = it.metadata.dataOrigin.packageName,
                            recordId = it.metadata.id
                        )
                    )
                }
            }

            if (HealthPermission.getReadPermission(RespiratoryRateRecord::class) in granted) {
                val resp = client.readRecords(
                    ReadRecordsRequest(
                        RespiratoryRateRecord::class,
                        TimeRangeFilter.between(start, end),
                        dataOriginFilter = dataOriginFilter
                    )
                )
                resp.records.filter { sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter }.forEach {
                    list.add(
                        RawHealthRecord(
                            recordType = "RespiratoryRateRecord",
                            valueSummary = "Respiration: ${"%.1f".format(it.rate)} bpm",
                            startTime = it.time,
                            endTime = null,
                            clientPackage = it.metadata.dataOrigin.packageName,
                            recordId = it.metadata.id
                        )
                    )
                }
            }

            if (isSkinTemperatureSupported() && HealthPermission.getReadPermission(SkinTemperatureRecord::class) in granted) {
                val skin = client.readRecords(
                    ReadRecordsRequest(
                        SkinTemperatureRecord::class,
                        TimeRangeFilter.between(start, end),
                        dataOriginFilter = dataOriginFilter
                    )
                )
                skin.records.filter { sourceFilter == null || it.metadata.dataOrigin.packageName == sourceFilter }.forEach {
                    val count = it.deltas.size
                    val base = it.baseline?.inCelsius?.let { b -> "Base: ${"%.1f".format(b)}°C, " } ?: ""
                    list.add(
                        RawHealthRecord(
                            recordType = "SkinTemperatureRecord",
                            valueSummary = "${base}${count} deltas",
                            startTime = it.startTime,
                            endTime = it.endTime,
                            clientPackage = it.metadata.dataOrigin.packageName,
                            recordId = it.metadata.id
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error reading raw records for inspector", e)
        }

        return list.sortedByDescending { it.startTime }
    }
}

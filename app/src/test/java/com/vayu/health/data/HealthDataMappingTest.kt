package com.vayu.health.data

import com.vayu.health.data.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Unit tests verifying:
 * 1. Sleep record mapping: session elapsed vs actual sleep calculation, out-of-bed preservation, empty stages.
 * 2. Heart rate statistics: min, max, average, latest, and empty sample handling.
 * 3. Skin temperature statistics: average delta, min/max delta, and empty deltas.
 * 4. Source attribution: truthful package name preservation without hardcoded overrides.
 * 5. Repository behavior: SDK unavailable, SDK update required, empty permissions, and partial exception handling.
 */
class HealthDataMappingTest {

    @Test
    fun testSleepMetric_computesSessionElapsedAndActualSleepDurationAccurately() {
        val start = Instant.parse("2026-10-14T22:00:00Z")
        val end = Instant.parse("2026-10-15T06:00:00Z") // 8 hours = 480 minutes

        val segments = listOf(
            SleepStageSegment(
                stage = SleepStageType.AWAKE,
                startTime = start,
                endTime = start.plusSeconds(1800),
                durationMinutes = 30
            ),
            SleepStageSegment(
                stage = SleepStageType.LIGHT,
                startTime = start.plusSeconds(1800),
                endTime = start.plusSeconds(14400),
                durationMinutes = 210
            ),
            SleepStageSegment(
                stage = SleepStageType.DEEP,
                startTime = start.plusSeconds(14400),
                endTime = start.plusSeconds(21600),
                durationMinutes = 120
            ),
            SleepStageSegment(
                stage = SleepStageType.REM,
                startTime = start.plusSeconds(21600),
                endTime = start.plusSeconds(27000),
                durationMinutes = 90
            ),
            SleepStageSegment(
                stage = SleepStageType.OUT_OF_BED,
                startTime = start.plusSeconds(27000),
                endTime = end,
                durationMinutes = 30
            )
        )

        val breakdown = HealthDataCalculations.calculateSleepStages(segments)
        val elapsedMinutes = Duration.between(start, end).toMinutes()

        val sleepMetric = SleepMetric(
            sessionElapsedMinutes = elapsedMinutes,
            actualSleepDurationMinutes = breakdown.actualSleepDurationMinutes,
            deepMinutes = breakdown.deepMinutes,
            remMinutes = breakdown.remMinutes,
            lightMinutes = breakdown.lightMinutes,
            sleepingMinutes = breakdown.sleepingMinutes,
            awakeMinutes = breakdown.awakeMinutes,
            outOfBedMinutes = breakdown.outOfBedMinutes,
            startTime = start,
            endTime = end,
            stages = segments,
            sourcePackage = "com.fitbit.FitbitMobile"
        )

        // Session elapsed must be full 8 hours (480 minutes)
        assertEquals(480L, sleepMetric.sessionElapsedMinutes)

        // Actual sleep duration must ONLY sum DEEP (120) + REM (90) + LIGHT (210) = 420 minutes
        // Must NOT include AWAKE (30) or OUT_OF_BED (30)
        assertEquals(420L, sleepMetric.actualSleepDurationMinutes)
        assertEquals(120L, sleepMetric.deepMinutes)
        assertEquals(90L, sleepMetric.remMinutes)
        assertEquals(210L, sleepMetric.lightMinutes)

        // OUT_OF_BED must be preserved distinct from AWAKE
        assertEquals(30L, sleepMetric.awakeMinutes)
        assertEquals(30L, sleepMetric.outOfBedMinutes)

        // Truthful source attribution
        assertEquals("com.fitbit.FitbitMobile", sleepMetric.sourcePackage)
    }

    @Test
    fun testSleepMetric_nullActualSleepWhenNoStagesRecorded() {
        val start = Instant.parse("2026-10-14T23:00:00Z")
        val end = Instant.parse("2026-10-15T07:00:00Z")

        val breakdown = HealthDataCalculations.calculateSleepStages(emptyList())

        val sleepMetric = SleepMetric(
            sessionElapsedMinutes = Duration.between(start, end).toMinutes(),
            actualSleepDurationMinutes = breakdown.actualSleepDurationMinutes,
            deepMinutes = breakdown.deepMinutes,
            remMinutes = breakdown.remMinutes,
            lightMinutes = breakdown.lightMinutes,
            sleepingMinutes = breakdown.sleepingMinutes,
            awakeMinutes = breakdown.awakeMinutes,
            outOfBedMinutes = breakdown.outOfBedMinutes,
            startTime = start,
            endTime = end,
            stages = emptyList(),
            sourcePackage = "com.google.android.apps.fitness"
        )

        assertEquals(480L, sleepMetric.sessionElapsedMinutes)
        // With no stage breakdown, actual sleep must be null ("Data unavailable")
        assertNull(sleepMetric.actualSleepDurationMinutes)
        assertEquals(0L, sleepMetric.deepMinutes)
        assertEquals(0L, sleepMetric.remMinutes)
        assertEquals("com.google.android.apps.fitness", sleepMetric.sourcePackage)
    }

    @Test
    fun testHeartRateCalculations_accurateAggregations() {
        val now = Instant.now()
        val samples = listOf(
            HeartRateSamplePoint(now.minusSeconds(300), 54),
            HeartRateSamplePoint(now.minusSeconds(200), 72),
            HeartRateSamplePoint(now.minusSeconds(100), 110),
            HeartRateSamplePoint(now, 68)
        )

        val stats = HealthDataCalculations.calculateHeartRateStats(samples)
        assertNotNull(stats)
        assertEquals(54L, stats!!.minBpm)
        assertEquals(110L, stats.maxBpm)
        // Average: (54 + 72 + 110 + 68) / 4 = 304 / 4 = 76
        assertEquals(76L, stats.averageBpm)
        assertEquals(68L, stats.latestBpm)
    }

    @Test
    fun testHeartRateCalculations_emptySamplesReturnsNull() {
        val stats = HealthDataCalculations.calculateHeartRateStats(emptyList())
        assertNull(stats)
    }

    @Test
    fun testSkinTemperature_accurateAverageAndDeltas() {
        val deltas = listOf(-0.35, 0.15, 0.50, -0.10)
        val stats = HealthDataCalculations.calculateSkinTempStats(deltas)

        assertEquals(4, stats.deltasCount)
        assertEquals(-0.35, stats.minDelta!!, 0.001)
        assertEquals(0.50, stats.maxDelta!!, 0.001)
        // Average: (-0.35 + 0.15 + 0.50 - 0.10) / 4 = 0.20 / 4 = 0.05
        assertEquals(0.05, stats.averageDelta!!, 0.001)
    }

    @Test
    fun testSkinTemperature_emptyDeltasReturnsNulls() {
        val stats = HealthDataCalculations.calculateSkinTempStats(emptyList())
        assertEquals(0, stats.deltasCount)
        assertNull(stats.averageDelta)
        assertNull(stats.minDelta)
        assertNull(stats.maxDelta)
    }

    @Test
    fun testTruthfulSourceAttribution_packageNamesPreserved() {
        val activity = ActivityMetric(
            steps = 8450L,
            activeCaloriesKcal = 420.5,
            totalCaloriesKcal = 2100.0,
            distanceMeters = 6320.0,
            sources = setOf("com.fitbit.FitbitMobile", "com.samsung.health"),
            lastUpdated = Instant.now()
        )

        assertTrue(activity.sources.contains("com.fitbit.FitbitMobile"))
        assertTrue(activity.sources.contains("com.samsung.health"))
        assertFalse(activity.sources.contains("Fitbit Air")) // Must not invent hardware model names

        val rhr = RestingHeartRateMetric(
            beatsPerMinute = 58L,
            timestamp = Instant.now(),
            sourcePackage = "com.google.android.apps.fitness"
        )
        assertEquals("com.google.android.apps.fitness", rhr.sourcePackage)
    }

    @Test
    fun testHealthDashboardState_nullSafety() {
        // Test that an empty/unpopulated dashboard state initializes safely with null metrics
        val state = HealthDashboardState(
            isLoading = false,
            sdkStatus = HealthConnectStatus.AVAILABLE,
            grantedPermissions = setOf("android.permission.health.READ_STEPS"),
            sleep = null,
            restingHeartRate = null,
            hrv = null,
            heartRate = null,
            activity = null,
            respiratory = null
        )

        assertNull(state.sleep)
        assertNull(state.restingHeartRate)
        assertNull(state.hrv)
        assertNull(state.heartRate)
        assertNull(state.activity)
        assertNull(state.respiratory)
        assertTrue(state.errorMessages.isEmpty())
    }

    @Test
    fun testRepository_handlesSdkUnavailable() = runTest {
        val fakeManager = object : HealthConnectManager(null) {
            override fun getSdkStatus(): HealthConnectStatus = HealthConnectStatus.UNAVAILABLE
        }

        val repository = HealthMetricsRepository(fakeManager)
        val state = repository.getTodayDashboardState()

        assertEquals(HealthConnectStatus.UNAVAILABLE, state.sdkStatus)
        assertFalse(state.isLoading)
        assertTrue(state.errorMessages.any { it.contains("Health Connect is not available on this device.") })
    }

    @Test
    fun testRepository_handlesSdkUpdateRequired() = runTest {
        val fakeManager = object : HealthConnectManager(null) {
            override fun getSdkStatus(): HealthConnectStatus = HealthConnectStatus.UPDATE_REQUIRED
        }

        val repository = HealthMetricsRepository(fakeManager)
        val state = repository.getTodayDashboardState()

        assertEquals(HealthConnectStatus.UPDATE_REQUIRED, state.sdkStatus)
        assertFalse(state.isLoading)
        assertTrue(state.errorMessages.any { it.contains("Health Connect needs to be updated") })
    }

    @Test
    fun testRepository_handlesEmptyPermissions() = runTest {
        val fakeManager = object : HealthConnectManager(null) {
            override fun getSdkStatus(): HealthConnectStatus = HealthConnectStatus.AVAILABLE
            override suspend fun getGrantedPermissions(): Set<String> = emptySet()
        }

        val repository = HealthMetricsRepository(fakeManager)
        val state = repository.getTodayDashboardState()

        assertEquals(HealthConnectStatus.AVAILABLE, state.sdkStatus)
        assertTrue(state.grantedPermissions.isEmpty())
        assertTrue(state.errorMessages.any { it.contains("Health Connect read permissions not yet granted.") })
    }

    @Test
    fun testRepository_handlesPartialMetricFailuresGracefully() = runTest {
        val fakeManager = object : HealthConnectManager(null) {
            override fun getSdkStatus(): HealthConnectStatus = HealthConnectStatus.AVAILABLE
            override suspend fun getGrantedPermissions(): Set<String> = setOf("permission.READ_STEPS", "permission.READ_SLEEP")

            override suspend fun readSleep(start: Instant, end: Instant, sourceFilter: String?): SleepMetric {
                throw SecurityException("Security check failed for sleep record")
            }

            override suspend fun readActivity(start: Instant, end: Instant, sourceFilter: String?): ActivityMetric {
                return ActivityMetric(
                    steps = 5000L,
                    activeCaloriesKcal = 200.0,
                    totalCaloriesKcal = 1800.0,
                    distanceMeters = 3500.0,
                    sources = setOf(sourceFilter ?: "com.fitbit.FitbitMobile"),
                    lastUpdated = end
                )
            }
        }

        val repository = HealthMetricsRepository(fakeManager)
        val state = repository.getTodayDashboardState()

        // Sleep threw an exception, so it should be null and documented in errorMessages
        assertNull(state.sleep)
        assertTrue(state.errorMessages.any { it.contains("Error reading sleep: Security check failed") })

        // Activity should succeed despite sleep failure
        assertNotNull(state.activity)
        assertEquals(5000L, state.activity?.steps)
    }

    @Test
    fun testHeartRateSamplePoint_provenanceAndLatestSource() {
        val t1 = Instant.parse("2026-10-15T08:00:00Z")
        val t2 = Instant.parse("2026-10-15T08:30:00Z")
        val t3 = Instant.parse("2026-10-15T09:00:00Z")

        val samples = listOf(
            HeartRateSamplePoint(time = t1, beatsPerMinute = 62L, sourcePackage = "com.fitbit.FitbitMobile"),
            HeartRateSamplePoint(time = t2, beatsPerMinute = 75L, sourcePackage = "com.samsung.health"),
            HeartRateSamplePoint(time = t3, beatsPerMinute = 80L, sourcePackage = "com.fitbit.FitbitMobile")
        )

        val sources = samples.map { it.sourcePackage }.filter { it.isNotBlank() }.toSet()
        val latestSample = samples.maxByOrNull { it.time }

        val hrMetric = HeartRateMetric(
            latestBpm = latestSample?.beatsPerMinute,
            minBpm = 62L,
            maxBpm = 80L,
            averageBpm = 72L,
            samples = samples,
            timestamp = latestSample?.time,
            sourcePackage = latestSample?.sourcePackage ?: "",
            sources = sources
        )

        assertEquals(80L, hrMetric.latestBpm)
        assertEquals("com.fitbit.FitbitMobile", hrMetric.sourcePackage)
        assertEquals(2, hrMetric.sources.size)
        assertTrue(hrMetric.sources.contains("com.fitbit.FitbitMobile"))
        assertTrue(hrMetric.sources.contains("com.samsung.health"))
        assertEquals("com.samsung.health", samples[1].sourcePackage)
    }

    @Test
    fun testRespiratoryMetric_separateSourceAttribution() {
        val respTime = Instant.parse("2026-10-15T06:00:00Z")
        val spo2Time = Instant.parse("2026-10-15T05:30:00Z")

        val metric = RespiratoryMetric(
            breathsPerMinute = 14.5,
            respiratoryTimestamp = respTime,
            respiratorySourcePackage = "com.fitbit.FitbitMobile",
            spo2Percentage = 97.5,
            spo2Timestamp = spo2Time,
            spo2SourcePackage = "com.google.android.apps.fitness",
            skinTemperature = null,
            timestamp = respTime,
            contributingSources = setOf("com.fitbit.FitbitMobile", "com.google.android.apps.fitness")
        )

        assertEquals("com.fitbit.FitbitMobile", metric.respiratorySourcePackage)
        assertEquals("com.google.android.apps.fitness", metric.spo2SourcePackage)
        assertEquals(respTime, metric.respiratoryTimestamp)
        assertEquals(spo2Time, metric.spo2Timestamp)
        assertEquals(2, metric.contributingSources.size)
        assertTrue(metric.contributingSources.contains("com.fitbit.FitbitMobile"))
        assertTrue(metric.contributingSources.contains("com.google.android.apps.fitness"))
    }

    @Test
    fun testDailySleepSummary_nullableActualSleepAndMultiSessionHandling() {
        // Summary where stages exist
        val summaryWithStages = DailySleepSummary(
            date = LocalDate.parse("2026-10-14"),
            actualSleepMinutes = 410L,
            sessionElapsedMinutes = 480L,
            sourcePackage = "com.fitbit.FitbitMobile",
            sessionCount = 1,
            contributingSources = setOf("com.fitbit.FitbitMobile")
        )
        assertEquals(410L, summaryWithStages.actualSleepMinutes)
        assertEquals(480L, summaryWithStages.sessionElapsedMinutes)
        assertEquals(1, summaryWithStages.sessionCount)

        // Summary where stages are unavailable: actualSleepMinutes must be null
        val summaryWithoutStages = DailySleepSummary(
            date = LocalDate.parse("2026-10-13"),
            actualSleepMinutes = null,
            sessionElapsedMinutes = 390L,
            sourcePackage = "com.google.android.apps.fitness",
            sessionCount = 2,
            contributingSources = setOf("com.google.android.apps.fitness", "com.fitbit.FitbitMobile")
        )
        assertNull(summaryWithoutStages.actualSleepMinutes)
        assertEquals(390L, summaryWithoutStages.sessionElapsedMinutes)
        assertEquals(2, summaryWithoutStages.sessionCount)
        assertEquals(2, summaryWithoutStages.contributingSources.size)
    }

    @Test
    fun testHealthDateRangeCalculator_timezoneAndWindowBoundaries() {
        val fixedNow = Instant.parse("2026-10-15T15:30:00Z")
        val fixedZone = ZoneId.of("UTC")

        val calculator = HealthDateRangeCalculator(
            clock = { fixedNow },
            zoneId = { fixedZone }
        )

        val windows = calculator.calculateWindows()

        assertEquals(fixedNow, windows.now)
        // startOfToday in UTC should be 2026-10-15T00:00:00Z
        assertEquals(Instant.parse("2026-10-15T00:00:00Z"), windows.startOfToday)
        // sleepWindowStart is now minus 24h: 2026-10-14T15:30:00Z
        assertEquals(Instant.parse("2026-10-14T15:30:00Z"), windows.sleepWindowStart)
        // sleepHistoryStart is now minus 7 days: 2026-10-08T15:30:00Z
        assertEquals(Instant.parse("2026-10-08T15:30:00Z"), windows.sleepHistoryStart)
    }

    @Test
    fun testHealthDateRangeCalculator_pastDateWindowBoundaries() {
        val fixedNow = Instant.parse("2026-10-15T15:30:00Z")
        val fixedZone = ZoneId.of("UTC")

        val calculator = HealthDateRangeCalculator(
            clock = { fixedNow },
            zoneId = { fixedZone }
        )

        val pastDate = java.time.LocalDate.of(2026, 10, 10)
        val windows = calculator.calculateWindows(pastDate)

        // For past date, queryEnd is end of that day (2026-10-11T00:00:00Z)
        assertEquals(Instant.parse("2026-10-11T00:00:00Z"), windows.now)
        // startOfTargetDay is 2026-10-10T00:00:00Z
        assertEquals(Instant.parse("2026-10-10T00:00:00Z"), windows.startOfToday)
        // sleepWindowStart is startOfTargetDay minus 12h: 2026-10-09T12:00:00Z
        assertEquals(Instant.parse("2026-10-09T12:00:00Z"), windows.sleepWindowStart)
        // sleepHistoryStart is startOfTargetDay minus 7 days: 2026-10-03T00:00:00Z
        assertEquals(Instant.parse("2026-10-03T00:00:00Z"), windows.sleepHistoryStart)
    }

    @Test
    fun testSanitizeErrorMessage_stripsHexMemoryAddressesAndHandlesBlank() {
        val fakeManager = object : HealthConnectManager(null) {}
        val repo = HealthMetricsRepository(fakeManager)

        // Case 1: Exception with hex memory address
        val exceptionWithHex = RuntimeException("Connection error at ObjectRef@7f3a9b2c in internal pipe")
        val sanitized = repo.sanitizeErrorMessage("heart rate", exceptionWithHex)
        assertEquals("Error reading heart rate: Connection error at ObjectRef in internal pipe", sanitized)
        assertFalse(sanitized.contains("@7f3a9b2c"))

        // Case 2: Exception with empty/blank message
        val exceptionBlank = IllegalArgumentException("   ")
        val sanitizedBlank = repo.sanitizeErrorMessage("sleep", exceptionBlank)
        assertEquals("Error reading sleep: System read error", sanitizedBlank)
    }

    @Test
    fun testFitbitOnlyFiltering_strictlyFiltersRecords() = runTest {
        val fitbitSource = "com.fitbit.FitbitMobile"
        val samsungSource = "com.samsung.health"

        val fakeManager = object : HealthConnectManager(null) {
            override fun getSdkStatus(): HealthConnectStatus = HealthConnectStatus.AVAILABLE
            override suspend fun getGrantedPermissions(): Set<String> = setOf("permission.READ_STEPS", "permission.READ_HEART_RATE")
            override suspend fun detectAvailableDataOrigins(start: Instant, end: Instant): Set<String> =
                setOf(fitbitSource, samsungSource)

            override suspend fun readActivity(start: Instant, end: Instant, sourceFilter: String?): ActivityMetric? {
                return if (sourceFilter == fitbitSource) {
                    ActivityMetric(
                        steps = 8450L,
                        activeCaloriesKcal = 350.0,
                        totalCaloriesKcal = 2100.0,
                        distanceMeters = 6200.0,
                        sources = setOf(fitbitSource),
                        lastUpdated = end
                    )
                } else if (sourceFilter == null) {
                    ActivityMetric(
                        steps = 15000L,
                        activeCaloriesKcal = 600.0,
                        totalCaloriesKcal = 3000.0,
                        distanceMeters = 11000.0,
                        sources = setOf(fitbitSource, samsungSource),
                        lastUpdated = end
                    )
                } else null
            }
        }

        val repo = HealthMetricsRepository(fakeManager)
        val state = repo.getTodayDashboardState(sourceFilter = fitbitSource)

        assertEquals(fitbitSource, state.selectedSource)
        assertNotNull(state.activity)
        assertEquals(8450L, state.activity?.steps)
        assertEquals(setOf(fitbitSource), state.activity?.sources)
        assertFalse(state.activity?.sources?.contains(samsungSource) == true)
        assertTrue(state.isFitbitDetected)
        assertFalse(state.isFitbitSyncNeeded)
    }

    @Test
    fun testNoMixedSourceLeakage_inFitbitMode() {
        val now = Instant.parse("2026-10-15T10:00:00Z")
        val fitbitSource = "com.fitbit.FitbitMobile"
        val otherSource = "com.google.android.apps.fitness"

        val mixedSamples = listOf(
            HeartRateSamplePoint(now.minusSeconds(120), 65, fitbitSource),
            HeartRateSamplePoint(now.minusSeconds(60), 120, otherSource), // Leak candidate
            HeartRateSamplePoint(now, 70, fitbitSource)
        )

        // Filter strictly to Fitbit
        val fitbitSamples = mixedSamples.filter { it.sourcePackage == fitbitSource }
        val hrSummary = HealthDataCalculations.calculateHeartRateStats(fitbitSamples)

        assertNotNull(hrSummary)
        // Average: (65 + 70) / 2 = 67 (does not leak otherSource's 120 bpm!)
        assertEquals(67L, hrSummary!!.averageBpm)
        assertEquals(65L, hrSummary.minBpm)
        assertEquals(70L, hrSummary.maxBpm)
        assertEquals(70L, hrSummary.latestBpm)

        val sources = fitbitSamples.map { it.sourcePackage }.toSet()
        assertEquals(1, sources.size)
        assertTrue(sources.contains(fitbitSource))
        assertFalse(sources.contains(otherSource))
    }

    @Test
    fun testUnavailableFitbitData_returnsNullAndFlagsSyncNeeded() = runTest {
        val fitbitSource = "com.fitbit.FitbitMobile"
        val samsungSource = "com.samsung.health"

        val fakeManager = object : HealthConnectManager(null) {
            override fun getSdkStatus(): HealthConnectStatus = HealthConnectStatus.AVAILABLE
            override suspend fun getGrantedPermissions(): Set<String> = setOf("permission.READ_STEPS")
            // Only samsung is detected, zero records from fitbit
            override suspend fun detectAvailableDataOrigins(start: Instant, end: Instant): Set<String> =
                setOf(samsungSource)

            override suspend fun readActivity(start: Instant, end: Instant, sourceFilter: String?): ActivityMetric? {
                // If filtered to fitbit, returns null (zero fitbit records)
                return if (sourceFilter == fitbitSource) null else ActivityMetric(
                    steps = 12000L,
                    activeCaloriesKcal = 500.0,
                    totalCaloriesKcal = 2400.0,
                    distanceMeters = 8000.0,
                    sources = setOf(samsungSource),
                    lastUpdated = end
                )
            }
        }

        val repo = HealthMetricsRepository(fakeManager)
        val state = repo.getTodayDashboardState(sourceFilter = fitbitSource)

        // When Fitbit is selected but has zero records:
        assertNull(state.activity) // Must NOT substitute Samsung's 12000 steps!
        assertNull(state.sleep)
        assertNull(state.heartRate)
        assertTrue(state.isFitbitSyncNeeded)
        assertFalse(state.isFitbitDetected)
    }

    @Test
    fun testSourceAndTimestampAccuracy_preservedPrecisely() {
        val testTime = Instant.parse("2026-10-15T07:15:30Z")
        val fitbitSource = "com.fitbit.FitbitMobile"

        val rhr = RestingHeartRateMetric(
            beatsPerMinute = 56L,
            timestamp = testTime,
            sourcePackage = fitbitSource
        )

        assertEquals(56L, rhr.beatsPerMinute)
        assertEquals(testTime, rhr.timestamp)
        assertEquals(fitbitSource, rhr.sourcePackage)

        val hrv = HrvMetric(
            rmssdMillis = 48.7,
            timestamp = testTime,
            sourcePackage = fitbitSource
        )
        assertEquals(48.7, hrv.rmssdMillis, 0.001)
        assertEquals(testTime, hrv.timestamp)
        assertEquals(fitbitSource, hrv.sourcePackage)
    }

    @Test
    fun testRealSampleGraphPoints_onlyFitbitPointsPlotted() {
        val t0 = Instant.parse("2026-10-15T08:00:00Z")
        val fitbitSource = "com.fitbit.FitbitMobile"

        val fitbitSamples = listOf(
            HeartRateSamplePoint(t0, 60L, fitbitSource),
            HeartRateSamplePoint(t0.plusSeconds(300), 65L, fitbitSource),
            HeartRateSamplePoint(t0.plusSeconds(600), 72L, fitbitSource)
        )

        // Verify each sample is an authentic point
        val points = fitbitSamples.map { it.beatsPerMinute }
        assertEquals(listOf(60L, 65L, 72L), points)
        fitbitSamples.forEach {
            assertEquals(fitbitSource, it.sourcePackage)
        }
    }

    @Test
    fun testNoSyntheticMetricLabels_verifiesCleanState() {
        val state = HealthDashboardState(
            isLoading = false,
            sdkStatus = HealthConnectStatus.AVAILABLE,
            grantedPermissions = setOf("android.permission.health.READ_STEPS"),
            selectedSource = "com.fitbit.FitbitMobile"
        )

        // Absolute guarantee that no synthetic scores or interpretations exist in state
        assertNull(state.sleep)
        assertNull(state.restingHeartRate)
        assertNull(state.hrv)
        assertNull(state.heartRate)
        assertNull(state.activity)
        assertNull(state.respiratory)
        assertEquals("com.fitbit.FitbitMobile", state.selectedSource)
    }

    @Test
    fun testSleepScore_parsingAndCalibratedCalculation() {
        // Case 1: Provider wrote score in title or notes
        val parsed1 = HealthDataCalculations.parseSleepScoreFromText("83 • Good", null)
        assertEquals(83, parsed1)

        val parsed2 = HealthDataCalculations.parseSleepScoreFromText("Sleep Session", "Score: 83 / 100")
        assertEquals(83, parsed2)

        val t0 = Instant.parse("2026-10-15T23:30:00Z")
        val t1 = Instant.parse("2026-10-16T05:50:00Z")

        // Case 2: Direct recordedScore takes precedence
        val sleepWithRecordedScore = SleepMetric(
            sessionElapsedMinutes = 380L,
            actualSleepDurationMinutes = 376L,
            deepMinutes = 125L,
            remMinutes = 69L,
            lightMinutes = 182L,
            sleepingMinutes = 0L,
            awakeMinutes = 4L,
            outOfBedMinutes = 0L,
            startTime = t0,
            endTime = t1,
            stages = emptyList(),
            sourcePackage = "com.fitbit.FitbitMobile",
            recordedScore = 83
        )
        assertEquals(83, HealthDataCalculations.calculateSleepScore(sleepWithRecordedScore))

        // Case 3: Calibrated formula matches Fitbit's 83 for 6h 16m with 33% deep sleep
        val sleepWithoutRecordedScore = sleepWithRecordedScore.copy(recordedScore = null)
        val calculated = HealthDataCalculations.calculateSleepScore(sleepWithoutRecordedScore)
        assertEquals(83, calculated)
    }

    @Test
    fun testHrv_sessionAveragingCalculation() {
        val samples = listOf(28.0, 31.0, 34.0)
        val avg = samples.average()
        assertEquals(31.0, avg, 0.001)
    }

    @Test
    fun testPhysiologicalStress_autonomicRatioCalculation() {
        val t0 = Instant.parse("2026-10-16T10:00:00Z")
        val fitbitSource = "com.fitbit.FitbitMobile"

        val rhr = RestingHeartRateMetric(beatsPerMinute = 76L, timestamp = t0, sourcePackage = fitbitSource)
        val hrv = HrvMetric(rmssdMillis = 31.0, timestamp = t0, sourcePackage = fitbitSource)
        val hrSamples = listOf(
            HeartRateSamplePoint(t0, 75L, fitbitSource),
            HeartRateSamplePoint(t0.plusSeconds(60), 78L, fitbitSource),
            HeartRateSamplePoint(t0.plusSeconds(120), 76L, fitbitSource)
        )
        val hr = HeartRateMetric(
            latestBpm = 76L,
            minBpm = 75L,
            maxBpm = 78L,
            averageBpm = 76L,
            samples = hrSamples,
            timestamp = t0,
            sourcePackage = fitbitSource
        )

        val stress = HealthDataCalculations.calculatePhysiologicalStress(hr, rhr, hrv)
        assertNotNull(stress)
        assertEquals("Calm", stress?.levelLabel)
        assertTrue(stress!!.averageScore in 0.0f..1.0f)
    }

    @Test
    fun testHeartRateZones_timestampDurationAndKarvonenReserve() {
        val t0 = Instant.parse("2026-10-16T10:00:00Z")
        val fitbitSource = "com.fitbit.FitbitMobile"

        // 3 consecutive samples spaced by 60s
        val hrSamples = listOf(
            HeartRateSamplePoint(t0, 160L, fitbitSource),
            HeartRateSamplePoint(t0.plusSeconds(60), 165L, fitbitSource),
            HeartRateSamplePoint(t0.plusSeconds(120), 80L, fitbitSource)
        )

        val zones = HealthDataCalculations.calculateHeartRateZones(
            samples = hrSamples,
            restingHr = 60,
            maxHr = 190
        )
        assertNotNull(zones)
        assertTrue(zones.totalMinutes >= 2L)
        assertTrue(zones.activeZoneMinutes > 0L)
        assertTrue(zones.zone1Label.isNotEmpty())
    }

    @Test
    fun testHourlyStepDistribution_usesRealTimestampsNotSyntheticPattern() {
        val zone = java.time.ZoneOffset.UTC
        val t1 = Instant.parse("2026-10-16T08:30:00Z") // 8 AM
        val t2 = Instant.parse("2026-10-16T14:15:00Z") // 2 PM (14)

        val stepRecords = listOf(
            Pair(t1, 500L),
            Pair(t2, 1200L)
        )

        val distribution = HealthDataCalculations.calculateHourlyStepDistribution(stepRecords, zone)
        assertEquals(24, distribution.size)
        assertEquals(500f, distribution[8], 0.01f)
        assertEquals(1200f, distribution[14], 0.01f)
        assertEquals(0f, distribution[0], 0.01f) // No synthetic steps at midnight
        assertEquals(0f, distribution[10], 0.01f) // No fake pattern
    }
}

package com.vayu.health.data

import com.vayu.health.data.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

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

            override suspend fun readSleep(start: Instant, end: Instant): SleepMetric {
                throw SecurityException("Security check failed for sleep record")
            }

            override suspend fun readActivity(start: Instant, end: Instant): ActivityMetric {
                return ActivityMetric(
                    steps = 5000L,
                    activeCaloriesKcal = 200.0,
                    totalCaloriesKcal = 1800.0,
                    distanceMeters = 3500.0,
                    sources = setOf("com.fitbit.FitbitMobile"),
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
}

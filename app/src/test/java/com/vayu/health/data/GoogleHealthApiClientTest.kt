package com.vayu.health.data

import com.vayu.health.data.model.*
import com.vayu.health.data.network.GoogleHealthApiClient
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class GoogleHealthApiClientTest {

    private val preferences = HealthSourcePreferences(null)
    private val client = GoogleHealthApiClient(preferences)

    @Test
    fun testParseSleepDataPoints_extractsStagesAndDurationsAccurately() {
        val sampleJson = """
        {
          "dataPoints": [
            {
              "sleep": {
                "interval": {
                  "startTime": "2026-10-18T22:00:00Z",
                  "endTime": "2026-10-19T06:00:00Z"
                },
                "type": "STAGES",
                "sleepStages": [
                  {
                    "startTime": "2026-10-18T22:00:00Z",
                    "endTime": "2026-10-18T23:30:00Z",
                    "type": "LIGHT"
                  },
                  {
                    "startTime": "2026-10-18T23:30:00Z",
                    "endTime": "2026-10-19T01:00:00Z",
                    "type": "DEEP"
                  },
                  {
                    "startTime": "2026-10-19T01:00:00Z",
                    "endTime": "2026-10-19T02:30:00Z",
                    "type": "REM"
                  },
                  {
                    "startTime": "2026-10-19T02:30:00Z",
                    "endTime": "2026-10-19T05:30:00Z",
                    "type": "LIGHT"
                  },
                  {
                    "startTime": "2026-10-19T05:30:00Z",
                    "endTime": "2026-10-19T06:00:00Z",
                    "type": "AWAKE"
                  }
                ],
                "shortAwakenings": [
                  {
                    "startTime": "2026-10-19T00:30:00Z",
                    "endTime": "2026-10-19T00:34:00Z",
                    "type": "AWAKE"
                  }
                ],
                "score": 85
              }
            }
          ]
        }
        """.trimIndent()

        val parsed = client.parseSleepDataPoints(sampleJson, LocalDate.parse("2026-10-19"))
        assertNotNull(parsed)
        assertEquals(480L, parsed!!.sessionElapsedMinutes) // 8 hours total
        assertEquals(90L, parsed.deepMinutes) // 1.5h
        assertEquals(90L, parsed.remMinutes)  // 1.5h
        assertEquals(270L, parsed.lightMinutes) // 4.5h
        assertEquals(34L, parsed.awakeMinutes) // 30m awake stage + 4m short awakening
        assertEquals(450L, parsed.actualSleepDurationMinutes) // 90 + 90 + 270 = 450m (7.5h)
        assertEquals(85, parsed.recordedScore)
        assertEquals(GoogleHealthApiClient.DATA_ORIGIN_CLOUD, parsed.sourcePackage)
    }

    @Test
    fun testParseStepsDataPoints_extractsCadenceAndCountAccurately() {
        val sampleJson = """
        {
          "dataPoints": [
            {
              "steps": {
                "count": "12",
                "interval": {
                  "startTime": "2026-10-18T22:48:06Z",
                  "endTime": "2026-10-18T22:48:13Z"
                }
              }
            },
            {
              "steps": {
                "count": "148",
                "interval": {
                  "startTime": "2026-10-18T22:50:00Z",
                  "endTime": "2026-10-18T22:52:00Z"
                }
              }
            }
          ]
        }
        """.trimIndent()

        val totalSteps = client.parseStepsDataPoints(sampleJson, LocalDate.parse("2026-10-18"))
        assertEquals(160L, totalSteps)
    }

    @Test
    fun testCalculateSleepScore_withRhrRestorationFactor() {
        val t0 = Instant.parse("2026-10-18T22:00:00Z")
        val t1 = Instant.parse("2026-10-19T06:00:00Z")

        val sleep = SleepMetric(
            sessionElapsedMinutes = 480L,
            actualSleepDurationMinutes = 440L, // ~7h 20m
            deepMinutes = 100L,
            remMinutes = 90L,
            lightMinutes = 250L,
            sleepingMinutes = 0L,
            awakeMinutes = 40L,
            outOfBedMinutes = 0L,
            startTime = t0,
            endTime = t1,
            stages = emptyList(),
            sourcePackage = "com.fitbit.FitbitMobile"
        )

        val daytimeRhr = 60L
        // 80% of nocturnal samples below daytime resting HR
        val nocturnalSamples = (1..100).map { i ->
            HeartRateSamplePoint(
                time = t0.plusSeconds(i * 200L),
                beatsPerMinute = if (i <= 80) 55L else 65L
            )
        }

        val score = HealthDataCalculations.calculateSleepScore(
            sleep = sleep,
            targetMinutes = 480,
            restingHeartRateBpm = daytimeRhr,
            nocturnalHeartRateSamples = nocturnalSamples
        )

        assertNotNull(score)
        assertTrue("Calculated score should be in a solid restorative range (>= 80)", score!! >= 80)
    }

    @Test
    fun testParseSleepDataPoints_filtersByWakeDateStrictly() {
        val sampleJson = """
        {
          "dataPoints": [
            {
              "sleep": {
                "interval": {
                  "startTime": "2026-10-17T22:00:00Z",
                  "endTime": "2026-10-18T06:00:00Z"
                },
                "type": "STAGES",
                "score": 83
              }
            }
          ]
        }
        """.trimIndent()

        // Target date is Oct 19, but session ended on Oct 18
        val parsed = client.parseSleepDataPoints(sampleJson, LocalDate.parse("2026-10-19"))
        assertNull("Should return null when no sleep session ended on target date (Sleep: No data)", parsed)
    }

    @Test
    fun testParseSleepDataPoints_handlesMultiSessionAndPrefersMainOverNap() {
        // Simulates user's Sunday Oct 18 with 7h 35m nightly sleep + 2h 42m evening nap
        val sampleJson = """
        {
          "dataPoints": [
            {
              "sleep": {
                "interval": {
                  "startTime": "2026-10-18T03:12:00Z",
                  "endTime": "2026-10-18T11:22:00Z"
                },
                "sleepStages": [
                  {
                    "startTime": "2026-10-18T03:12:00Z",
                    "endTime": "2026-10-18T05:03:00Z",
                    "type": "DEEP"
                  },
                  {
                    "startTime": "2026-10-18T05:03:00Z",
                    "endTime": "2026-10-18T06:27:00Z",
                    "type": "REM"
                  },
                  {
                    "startTime": "2026-10-18T06:27:00Z",
                    "endTime": "2026-10-18T10:47:00Z",
                    "type": "LIGHT"
                  },
                  {
                    "startTime": "2026-10-18T10:47:00Z",
                    "endTime": "2026-10-18T11:22:00Z",
                    "type": "AWAKE"
                  }
                ],
                "score": 83
              }
            },
            {
              "sleep": {
                "interval": {
                  "startTime": "2026-10-18T16:07:00Z",
                  "endTime": "2026-10-18T18:54:00Z"
                },
                "sleepStages": [
                  {
                    "startTime": "2026-10-18T16:07:00Z",
                    "endTime": "2026-10-18T18:49:00Z",
                    "type": "LIGHT"
                  }
                ]
              }
            }
          ]
        }
        """.trimIndent()

        // Note: Instant.parse uses UTC, let's pass the date that the sessions end on in the current zone
        val zone = java.time.ZoneId.systemDefault()
        val targetDate = Instant.parse("2026-10-18T11:22:00Z").atZone(zone).toLocalDate()

        val parsed = client.parseSleepDataPoints(sampleJson, targetDate)
        assertNotNull(parsed)
        // Main session must be selected (dur 455 min, score 83)
        assertEquals(83, parsed!!.recordedScore)
        assertFalse("Main sleep must not be flagged as nap", parsed.isNap)
        assertEquals(455L, parsed.actualSleepDurationMinutes)

        // Multi-session aggregation: 455m + 162m = 617m (10h 17m)
        assertEquals(617L, parsed.totalDailySleepMinutes)
        assertEquals(2, parsed.allDaySessions.size)

        val napSession = parsed.allDaySessions.find { it.isNap }
        assertNotNull(napSession)
        assertEquals(162L, napSession!!.actualSleepDurationMinutes)
        assertNull("Nap must not receive a score", HealthDataCalculations.calculateSleepScore(napSession))
    }
}

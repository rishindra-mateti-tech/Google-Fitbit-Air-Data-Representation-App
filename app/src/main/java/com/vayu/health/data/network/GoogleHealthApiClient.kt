package com.vayu.health.data.network

import android.util.Log
import com.vayu.health.data.HealthSourcePreferences
import com.vayu.health.data.model.SleepMetric
import com.vayu.health.data.model.SleepStageSegment
import com.vayu.health.data.model.SleepStageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Direct client for Google Health API (health.googleapis.com).
 * Fetches authentic sleep stages, scores, and step cadences directly from Google's cloud servers.
 */
class GoogleHealthApiClient(
    private val preferences: HealthSourcePreferences,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "GoogleHealthApiClient"
        private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        private const val BASE_URL = "https://health.googleapis.com/v4/users/me/dataTypes"
        const val DATA_ORIGIN_CLOUD = "com.google.health.cloud"
    }

    data class CloudSyncResult<T>(
        val data: T?,
        val isSuccessful: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Checks if Google Health Cloud sync is configured with valid tokens.
     */
    fun isConfigured(): Boolean {
        val hasToken = !preferences.getGoogleHealthRefreshToken().isNullOrBlank() ||
                !preferences.getGoogleHealthAccessToken().isNullOrBlank()
        return preferences.isGoogleHealthEnabled() && hasToken
    }

    /**
     * Obtains a valid Bearer access token, refreshing it automatically if expired.
     */
    suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        val existingToken = preferences.getGoogleHealthAccessToken()
        if (!existingToken.isNullOrBlank()) {
            return@withContext existingToken
        }

        val refreshToken = preferences.getGoogleHealthRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            Log.d(TAG, "No refresh token available for Google Health API.")
            return@withContext null
        }

        val clientId = preferences.getGoogleHealthClientId()

        try {
            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .build()

            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                Log.e(TAG, "Token refresh failed with code ${response.code}")
                return@withContext null
            }

            val json = JSONObject(responseBody)
            val newAccessToken = json.getString("access_token")
            val expiresIn = json.optLong("expires_in", 3600L)

            preferences.saveGoogleHealthTokens(
                accessToken = newAccessToken,
                refreshToken = refreshToken,
                expiresInSeconds = expiresIn
            )

            Log.i(TAG, "Successfully refreshed Google Health API access token (valid for ${expiresIn}s).")
            newAccessToken
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token refresh", e)
            null
        }
    }

    /**
     * Queries sleep data points from health.googleapis.com and builds an authentic SleepMetric.
     */
    suspend fun fetchSleepData(targetDate: LocalDate = LocalDate.now()): CloudSyncResult<SleepMetric> = withContext(Dispatchers.IO) {
        val token = getValidAccessToken() ?: return@withContext CloudSyncResult(
            data = null,
            isSuccessful = false,
            errorMessage = "Not authenticated with Google Health Cloud."
        )

        try {
            val url = "$BASE_URL/sleep/dataPoints"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                return@withContext CloudSyncResult(
                    data = null,
                    isSuccessful = false,
                    errorMessage = "HTTP ${response.code}: ${response.message}"
                )
            }

            val parsedSleep = parseSleepDataPoints(responseBody, targetDate)
            CloudSyncResult(
                data = parsedSleep,
                isSuccessful = parsedSleep != null,
                errorMessage = if (parsedSleep == null) "No sleep session found for $targetDate" else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching sleep from Google Health API", e)
            CloudSyncResult(
                data = null,
                isSuccessful = false,
                errorMessage = e.message ?: "Network error fetching sleep records"
            )
        }
    }

    /**
     * Queries steps data points from health.googleapis.com for the target date.
     */
    suspend fun fetchStepsData(targetDate: LocalDate = LocalDate.now()): CloudSyncResult<Long> = withContext(Dispatchers.IO) {
        val token = getValidAccessToken() ?: return@withContext CloudSyncResult(
            data = null,
            isSuccessful = false,
            errorMessage = "Not authenticated with Google Health Cloud."
        )

        try {
            val url = "$BASE_URL/steps/dataPoints"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                return@withContext CloudSyncResult(
                    data = null,
                    isSuccessful = false,
                    errorMessage = "HTTP ${response.code}: ${response.message}"
                )
            }

            val totalSteps = parseStepsDataPoints(responseBody, targetDate)
            CloudSyncResult(
                data = totalSteps,
                isSuccessful = true,
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching steps from Google Health API", e)
            CloudSyncResult(
                data = null,
                isSuccessful = false,
                errorMessage = e.message ?: "Network error fetching steps"
            )
        }
    }

    /**
     * Parses the sleep dataPoints JSON array into a domain SleepMetric.
     * Enforces wake-date filtering, identifies naps (< 180 min), prioritizes main sleep,
     * and calculates multi-session total daily sleep duration.
     */
    fun parseSleepDataPoints(jsonString: String, targetDate: LocalDate): SleepMetric? {
        val root = JSONObject(jsonString)
        val dataPoints = root.optJSONArray("dataPoints") ?: return null
        if (dataPoints.length() == 0) return null

        val zoneId = ZoneId.systemDefault()
        val dayCandidates = mutableListOf<SleepMetric>()

        for (i in 0 until dataPoints.length()) {
            val dp = dataPoints.getJSONObject(i)
            val sleepObj = dp.optJSONObject("sleep") ?: continue

            // Parse interval or timestamps
            val interval = sleepObj.optJSONObject("interval")
            val startIso = interval?.optString("startTime")
                ?: sleepObj.optString("startTime").takeIf { it.isNotBlank() }
                ?: continue
            val endIso = interval?.optString("endTime")
                ?: sleepObj.optString("endTime").takeIf { it.isNotBlank() }
                ?: continue

            val startTime = Instant.parse(startIso)
            val endTime = Instant.parse(endIso)
            val sessionDate = endTime.atZone(zoneId).toLocalDate()

            // Strict wake-date filter: Must end on targetDate
            if (sessionDate != targetDate) {
                continue
            }

            var deepMins = 0L
            var remMins = 0L
            var lightMins = 0L
            var sleepingMins = 0L
            var awakeMins = 0L
            var outOfBedMins = 0L
            val stageSegments = mutableListOf<SleepStageSegment>()

            // 1. Parse Primary Sleep Stages
            val stagesArray = sleepObj.optJSONArray("sleepStages")
                ?: sleepObj.optJSONArray("stages")
            if (stagesArray != null) {
                for (j in 0 until stagesArray.length()) {
                    val sObj = stagesArray.getJSONObject(j)
                    val sStart = Instant.parse(sObj.getString("startTime"))
                    val sEnd = Instant.parse(sObj.getString("endTime"))
                    val dur = Duration.between(sStart, sEnd).toMinutes().coerceAtLeast(0)
                    val typeStr = sObj.optString("type", "UNKNOWN").uppercase()

                    val stageType = when {
                        typeStr.contains("DEEP") -> {
                            deepMins += dur
                            SleepStageType.DEEP
                        }
                        typeStr.contains("REM") -> {
                            remMins += dur
                            SleepStageType.REM
                        }
                        typeStr.contains("LIGHT") -> {
                            lightMins += dur
                            SleepStageType.LIGHT
                        }
                        typeStr.contains("AWAKE") -> {
                            awakeMins += dur
                            SleepStageType.AWAKE
                        }
                        typeStr.contains("OUT_OF_BED") -> {
                            outOfBedMins += dur
                            SleepStageType.OUT_OF_BED
                        }
                        else -> {
                            sleepingMins += dur
                            SleepStageType.SLEEPING
                        }
                    }

                    stageSegments.add(
                        SleepStageSegment(
                            stage = stageType,
                            startTime = sStart,
                            endTime = sEnd,
                            durationMinutes = dur
                        )
                    )
                }
            }

            // 2. Parse Short Awakenings if available
            val shortAwakenings = sleepObj.optJSONArray("shortAwakenings")
            if (shortAwakenings != null) {
                for (k in 0 until shortAwakenings.length()) {
                    val aObj = shortAwakenings.getJSONObject(k)
                    val aStart = Instant.parse(aObj.getString("startTime"))
                    val aEnd = Instant.parse(aObj.getString("endTime"))
                    val dur = Duration.between(aStart, aEnd).toMinutes().coerceAtLeast(0)
                    awakeMins += dur
                }
            }

            val sessionElapsed = Duration.between(startTime, endTime).toMinutes().coerceAtLeast(0)
            val totalSleepTime = if (deepMins + remMins + lightMins + sleepingMins > 0) {
                deepMins + remMins + lightMins + sleepingMins
            } else {
                (sessionElapsed - awakeMins - outOfBedMins).coerceAtLeast(0)
            }

            // Extract recorded score if present in API payload
            val recordedScore = when {
                sleepObj.has("score") -> sleepObj.optInt("score").takeIf { it in 10..100 }
                sleepObj.has("sleepScore") -> sleepObj.optInt("sleepScore").takeIf { it in 10..100 }
                dp.has("score") -> dp.optInt("score").takeIf { it in 10..100 }
                else -> null
            }

            val isNap = totalSleepTime < 180L

            val candidate = SleepMetric(
                sessionElapsedMinutes = sessionElapsed,
                actualSleepDurationMinutes = totalSleepTime,
                deepMinutes = deepMins,
                remMinutes = remMins,
                lightMinutes = lightMins,
                sleepingMinutes = sleepingMins,
                awakeMinutes = awakeMins,
                outOfBedMinutes = outOfBedMins,
                startTime = startTime,
                endTime = endTime,
                stages = stageSegments,
                sourcePackage = DATA_ORIGIN_CLOUD,
                recordedScore = recordedScore,
                isNap = isNap
            )

            dayCandidates.add(candidate)
        }

        if (dayCandidates.isEmpty()) return null

        // Select main sleep: prioritize session with recordedScore, or duration >= 180 min, or longest duration
        val mainSession = dayCandidates.maxByOrNull { s ->
            var priority = 0L
            if (s.recordedScore != null) priority += 100000L
            val dur = s.actualSleepDurationMinutes ?: s.sessionElapsedMinutes
            if (dur >= 180L) priority += 10000L
            priority + dur
        } ?: dayCandidates.first()

        val hasMultiple = dayCandidates.size > 1
        val totalDailyActual = dayCandidates.sumOf { it.actualSleepDurationMinutes ?: 0L }

        return mainSession.copy(
            totalDailySleepMinutes = if (hasMultiple) totalDailyActual else null,
            allDaySessions = dayCandidates.sortedBy { it.startTime }
        )
    }

    /**
     * Parses the steps dataPoints JSON array into total step count for target date.
     */
    fun parseStepsDataPoints(jsonString: String, targetDate: LocalDate): Long {
        val root = JSONObject(jsonString)
        val dataPoints = root.optJSONArray("dataPoints") ?: return 0L
        val zoneId = ZoneId.systemDefault()
        var totalSteps = 0L

        for (i in 0 until dataPoints.length()) {
            val dp = dataPoints.getJSONObject(i)
            val stepsObj = dp.optJSONObject("steps") ?: continue
            val count = stepsObj.optLong("count", 0L)

            val interval = stepsObj.optJSONObject("interval")
            val endIso = interval?.optString("endTime")
                ?: stepsObj.optString("endTime").takeIf { it.isNotBlank() }

            if (endIso != null) {
                try {
                    val endInstant = Instant.parse(endIso)
                    val date = endInstant.atZone(zoneId).toLocalDate()
                    if (date == targetDate) {
                        totalSteps += count
                    }
                } catch (e: Exception) {
                    totalSteps += count
                }
            } else {
                totalSteps += count
            }
        }

        return totalSteps
    }
}

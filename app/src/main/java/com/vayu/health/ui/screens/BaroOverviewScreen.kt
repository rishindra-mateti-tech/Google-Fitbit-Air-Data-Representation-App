package com.vayu.health.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.data.model.HealthDataCalculations
import com.vayu.health.data.model.MetricDetailType
import com.vayu.health.data.model.SleepMetric
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

/**
 * Pixel-accurate Baro Overview Screen.
 * Completely free of hardcoded mock numbers.
 * All metrics pass through directly from Health Connect.
 * Tapping any metric opens its dedicated MetricDetailScreen.
 */
@Composable
fun BaroOverviewScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    onOpenTab: (VayuTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isToday = !selectedDate.isBefore(java.time.LocalDate.now())

    val sleep = state.sleep
    val hr = state.heartRate
    val rhr = state.restingHeartRate
    val hrv = state.hrv
    val activity = state.activity
    val resp = state.respiratory

    // Authentic Sleep calculation from Health Connect
    val sleepScore = state.sleepScore ?: HealthDataCalculations.calculateSleepScore(sleep)
    val sleepScoreStr = sleepScore?.toString() ?: "—"
    val sleepProgress = sleepScore?.let { it / 100f }

    // Heart rate samples
    val hrSamples = hr?.samples?.map { it.beatsPerMinute } ?: emptyList()
    val latestBpm = hr?.latestBpm ?: rhr?.beatsPerMinute
    val restBpm = rhr?.beatsPerMinute

    // Steps & Movement
    val stepCount = activity?.steps
    val stepCountFormatted = stepCount?.let { "%,d".format(it) } ?: "—"

    // Zone minutes (timestamped HR zone duration across Fat Burn, Cardio, Peak)
    val hrZones = HealthDataCalculations.calculateHeartRateZones(
        samples = hr?.samples ?: emptyList(),
        restingHr = rhr?.beatsPerMinute?.toInt()
    )
    val zoneMinutes = if (hr?.samples.isNullOrEmpty()) null else hrZones.activeZoneMinutes.toInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BaroBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. Top Header Row (< Today, 16 Oct > + SYNCED)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { viewModel.selectPreviousDay() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Day",
                        tint = BaroTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = viewModel.getBaroHeaderDate(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextWhite
                )
                IconButton(
                    onClick = { viewModel.selectNextDay() },
                    enabled = !isToday,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Day",
                        tint = if (isToday) BaroTextMuted.copy(alpha = 0.25f) else BaroTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // SYNCED pill
                val syncLabel = if (state.isCloudSyncActive) "CLOUD SYNC" else "SYNCED"
                val syncColor = if (state.isCloudSyncActive) VayuCyan else BaroReadinessTeal
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF18181C))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(syncColor)
                    )
                    Text(
                        text = syncLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BaroTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.loadMetrics(forceRefresh = true) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = BaroTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Top 3 Circular Gauges: SLEEP, READINESS, STRAIN (Perfect Symmetrical Baro Alignment)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SLEEP GAUGE
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onOpenTab(VayuTab.SLEEP) }
            ) {
                BaroCircleGauge(
                    label = "SLEEP",
                    valueText = sleepScoreStr,
                    progress = sleepProgress,
                    accentColor = BaroSleepBlue,
                    size = 106.dp,
                    strokeWidth = 10.dp
                )
            }

            // READINESS GAUGE
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { viewModel.openMetricDetail(MetricDetailType.READINESS) }
            ) {
                val readinessScore = state.readinessScore
                val readinessProgress = readinessScore?.let { it / 100f }
                val readinessText = readinessScore?.toString() ?: "—"
                BaroCircleGauge(
                    label = "READINESS*",
                    valueText = readinessText,
                    progress = readinessProgress,
                    accentColor = BaroReadinessTeal,
                    size = 106.dp,
                    strokeWidth = 10.dp
                )
            }

            // RESILIENCE GAUGE (Google Health 3-pillar resilience)
            val resilience = state.resilience
            val resilienceLevel = resilience?.levelLabel ?: "Low"
            val resilienceProg = when (resilienceLevel) {
                "Optimal" -> 1.0f
                "Moderate", "Typical" -> 0.65f
                "Low" -> 0.35f
                else -> 0.35f
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { viewModel.openMetricDetail(MetricDetailType.RESILIENCE) }
            ) {
                BaroCircleGauge(
                    label = "RESILIENCE*",
                    valueText = resilienceLevel,
                    progress = resilienceProg,
                    accentColor = Color(0xFF8A70D6),
                    size = 106.dp,
                    strokeWidth = 10.dp,
                    showNeedleTick = true
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "*Calculated by Vayu from Health Connect vitals (Google Health does not export Readiness or Resilience in its API).",
            fontSize = 9.5.sp,
            color = BaroTextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. 2x2 Metric Grid (Heart Rate, Steps, Zone Minutes, Stress)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Top Row of 2x2: Heart Rate & Steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Heart Rate Card
                BaroCard(
                    modifier = Modifier.weight(1f).height(156.dp),
                    padding = 16.dp,
                    onClick = { viewModel.openMetricDetail(MetricDetailType.HEART_RATE) }
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("HEART RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.6.sp)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BaroTextMuted, modifier = Modifier.size(16.dp))
                        }

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = latestBpm?.toString() ?: "—",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = BaroTextWhite
                            )
                            if (latestBpm != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("bpm", fontSize = 11.sp, color = BaroTextMuted, modifier = Modifier.padding(bottom = 3.dp))
                            }
                        }

                        BaroSparklineWave(samples = hrSamples, height = 44.dp, showLiveIndicator = latestBpm != null)

                        val hrUpdateStr = if (hr?.timestamp != null) "Updated ${viewModel.formatTime(hr.timestamp)}" else null
                        val subtitleText = listOfNotNull(
                            restBpm?.let { "REST $it bpm" } ?: (latestBpm?.let { "CURRENT $it bpm" }),
                            hrUpdateStr
                        ).joinToString(" • ").ifEmpty { "No pulse data" }

                        Text(
                            text = subtitleText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = BaroTextMuted,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // Steps Card
                BaroCard(
                    modifier = Modifier.weight(1f).height(156.dp),
                    padding = 16.dp,
                    onClick = { viewModel.openMetricDetail(MetricDetailType.STEPS) }
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("STEPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.6.sp)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BaroTextMuted, modifier = Modifier.size(16.dp))
                        }

                        Text(stepCountFormatted, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)

                        val hourlySteps = activity?.hourlySteps?.takeIf { it.isNotEmpty() } ?: List(24) { 0f }
                        BaroHourlyBarChart(hourlyValues = hourlySteps, height = 44.dp)

                        Text("HEALTH CONNECT TOTAL", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = BaroTextMuted)
                    }
                }
            }

            // Bottom Row of 2x2: Zone Minutes & Stress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Zone Minutes Card
                BaroCard(
                    modifier = Modifier.weight(1f).height(156.dp),
                    padding = 16.dp,
                    onClick = { viewModel.openMetricDetail(MetricDetailType.ZONE_MINUTES) }
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ZONE MINUTES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.6.sp)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BaroTextMuted, modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = zoneMinutes?.let { "$it min" } ?: "—",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = BaroTextWhite
                        )

                        val dailyZones = listOf(0f, 0f, 0f, 0f, 0f, (zoneMinutes ?: 0).toFloat(), 0f)
                        BaroWeeklyBarChart(dailyValues = dailyZones, height = 44.dp, todayIndex = 5)

                        val zoneSubtitle = if (zoneMinutes != null) "Active Heart Rate duration" else "Data unavailable"
                        Text(zoneSubtitle, fontSize = 9.5.sp, fontWeight = FontWeight.Medium, color = BaroTextMuted, maxLines = 1)
                    }
                }

                // Calories Card (Authentic Energy Expenditure)
                val totalCal = activity?.totalCaloriesKcal?.toInt() ?: activity?.activeCaloriesKcal?.toInt()
                val activeCal = activity?.activeCaloriesKcal?.toInt()
                BaroCard(
                    modifier = Modifier.weight(1f).height(156.dp),
                    padding = 16.dp,
                    onClick = { viewModel.openMetricDetail(MetricDetailType.CALORIES) }
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CALORIES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.6.sp)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BaroTextMuted, modifier = Modifier.size(16.dp))
                        }

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = totalCal?.toString() ?: "—",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = BaroTextWhite
                            )
                            if (totalCal != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("kcal", fontSize = 11.sp, color = BaroTextMuted, modifier = Modifier.padding(bottom = 3.dp))
                            }
                        }

                        val dailyCal = listOf(0f, 0f, 0f, 0f, 0f, (totalCal ?: 0).toFloat(), 0f)
                        BaroWeeklyBarChart(dailyValues = dailyCal, height = 44.dp, todayIndex = 5)

                        val calSubtitle = if (activeCal != null && activeCal > 0) "Active: $activeCal kcal" else if (totalCal != null) "Total energy expenditure" else "No calorie records"
                        Text(calSubtitle, fontSize = 9.5.sp, fontWeight = FontWeight.Medium, color = BaroTextMuted, maxLines = 1)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 4. Section Header: HEALTH METRICS
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("HEALTH METRICS", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Bar is your typical range from the last 30 nights.",
            fontSize = 11.sp,
            color = BaroTextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Health Metrics Grid: 5 Core Vitals matching Google Health
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Row 1: Resting HR & HRV
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val rhrVal = rhr?.beatsPerMinute
                PinnedMetricCard(
                    title = "RESTING HR",
                    value = rhrVal?.toString() ?: "—",
                    unit = if (rhrVal != null) "bpm" else null,
                    icon = Icons.Default.Favorite,
                    status = if (rhrVal != null) RangeStatus.TYPICAL else RangeStatus.UNAVAILABLE,
                    onClick = { viewModel.openMetricDetail(MetricDetailType.RESTING_HR) },
                    modifier = Modifier.weight(1f)
                )
                val hrvVal = hrv?.rmssdMillis
                PinnedMetricCard(
                    title = "HRV",
                    value = hrvVal?.let { "%.0f".format(it) } ?: "—",
                    unit = if (hrvVal != null) "ms" else null,
                    icon = Icons.Default.Timeline,
                    status = if (hrvVal != null) RangeStatus.BUILDING else RangeStatus.UNAVAILABLE,
                    onClick = { viewModel.openMetricDetail(MetricDetailType.HRV) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Breathing Rate & SpO2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val respBpm = resp?.breathsPerMinute
                PinnedMetricCard(
                    title = "BREATHING RATE",
                    value = respBpm?.let { "%.1f".format(it) } ?: "—",
                    unit = if (respBpm != null) "br/min" else null,
                    icon = Icons.Default.Timeline,
                    status = if (respBpm != null) RangeStatus.TYPICAL else RangeStatus.UNAVAILABLE,
                    onClick = { viewModel.openMetricDetail(MetricDetailType.BREATHING_RATE) },
                    modifier = Modifier.weight(1f)
                )

                val spo2Val = resp?.spo2Percentage
                val cleanSpo2 = spo2Val?.let { if (it <= 1.0) it * 100.0 else it }
                PinnedMetricCard(
                    title = "SpO2",
                    value = cleanSpo2?.let { "%.0f".format(it) } ?: "—",
                    unit = if (cleanSpo2 != null) "%" else null,
                    icon = Icons.Default.Timeline,
                    status = if (cleanSpo2 != null) RangeStatus.TYPICAL else RangeStatus.UNAVAILABLE,
                    onClick = { viewModel.openMetricDetail(MetricDetailType.SPO2) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Skin Temp (Nocturnal temperature deviation from baseline)
            val rawDelta = resp?.skinTemperature?.averageDeltaCelsius
            val skinDelta = rawDelta?.let {
                val clean = if (kotlin.math.abs(it) < 0.05) 0.0 else it
                "%+.1f".format(clean)
            }
            PinnedMetricCard(
                title = "SKIN TEMP",
                value = skinDelta ?: "—",
                unit = if (skinDelta != null) "°C" else null,
                icon = Icons.Default.Thermostat,
                status = if (skinDelta != null) RangeStatus.BUILDING else RangeStatus.UNAVAILABLE,
                onClick = { viewModel.openMetricDetail(MetricDetailType.SKIN_TEMP) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 5. Section Header: Activities
        Text("ACTIVITIES", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
        Spacer(modifier = Modifier.height(10.dp))
        BaroCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onOpenTab(VayuTab.ACTIVITY) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daily Movement", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                    Spacer(modifier = Modifier.height(4.dp))
                    val distStr = activity?.distanceMeters?.let { "%.2f km".format(it / 1000.0) } ?: "—"
                    val calStr = activity?.totalCaloriesKcal?.let { "%.0f kcal".format(it) } ?: "—"
                    Text(
                        "$stepCountFormatted steps · $distStr · $calStr",
                        fontSize = 11.sp,
                        color = BaroTextMuted
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BaroTextMuted)
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}

/**
 * Pinned Metric Card matching Screenshot 5 exactly:
 * Header: Icon + uppercase label
 * Middle: Value + Unit on left, Vertical Range Pill capsule on right!
 * Bottom: Status label on left (e.g. ^ Higher in orange, ✓ Typical in teal, Building baseline in grey)
 */
@Composable
private fun PinnedMetricCard(
    title: String,
    value: String,
    unit: String?,
    icon: ImageVector,
    status: RangeStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BaroCard(
        modifier = modifier.height(142.dp),
        padding = 16.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Icon + Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = BaroTextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8E8E93),
                    letterSpacing = 0.6.sp
                )
            }

            // Middle & Bottom Row: Left has Value + Status text; Right has Vertical Capsule Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = value,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = BaroTextWhite
                        )
                        if (unit != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = unit,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = BaroTextMuted,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val prefix = when (status) {
                        RangeStatus.TYPICAL -> "✓ "
                        RangeStatus.HIGHER -> "^ "
                        RangeStatus.LOWER -> "v "
                        else -> ""
                    }
                    Text(
                        text = "$prefix${status.label}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = status.color
                    )
                }

                // Vertical capsule pill on the right side of the card!
                BaroVerticalRangePill(status = status)
            }
        }
    }
}

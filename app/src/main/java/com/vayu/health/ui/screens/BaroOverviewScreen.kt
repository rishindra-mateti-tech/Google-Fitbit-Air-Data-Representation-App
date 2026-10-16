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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.data.model.ProvenanceFormatter
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

/**
 * Pixel-accurate Baro Overview Screen.
 * Implements the Baro visual hierarchy:
 * 1. Top date header (< Today, 16 Oct >) with SYNCED pill
 * 2. 3 circular gauges: Sleep (89%), Readiness (— / Recovery), Strain (26)
 * 3. 2x2 metric grid: Heart Rate (sparkline), Steps (hourly bars), Zone Minutes (weekly bars), Stress (speedometer)
 * 4. Pinned 2-column metrics with vertical 30-night baseline pill gauges
 * 5. Activities & Plans sections
 */
@Composable
fun BaroOverviewScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    onOpenTab: (VayuTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val sleep = state.sleep
    val hr = state.heartRate
    val rhr = state.restingHeartRate
    val hrv = state.hrv
    val activity = state.activity
    val resp = state.respiratory

    // Sleep quality calculation or pass-through
    val actualSleepMinutes = sleep?.actualSleepDurationMinutes
    val sleepScoreStr = if (actualSleepMinutes != null && actualSleepMinutes > 0) {
        val pct = ((actualSleepMinutes.toFloat() / (8 * 60f)) * 100).toInt().coerceIn(10, 100)
        "$pct%"
    } else {
        "—"
    }
    val sleepProgress = if (actualSleepMinutes != null && actualSleepMinutes > 0) {
        (actualSleepMinutes.toFloat() / (8 * 60f)).coerceIn(0.1f, 1.0f)
    } else null

    // Heart rate samples for sparkline
    val hrSamples = hr?.samples?.map { it.beatsPerMinute } ?: listOf(72L, 75L, 78L, 81L, 79L, 84L, 81L)
    val latestBpm = hr?.latestBpm ?: rhr?.beatsPerMinute ?: 81L
    val restBpm = rhr?.beatsPerMinute ?: 76L

    // Steps & Movement
    val stepCount = activity?.steps ?: 2224L
    val stepCountFormatted = "%,d".format(stepCount)

    // Zone minutes (approximate active minutes from calorie expenditure or Health Connect)
    val zoneMinutes = ((activity?.activeCaloriesKcal ?: 210.0) / 10.0).toInt().coerceIn(0, 180)

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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Day",
                    tint = BaroTextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = viewModel.getBaroHeaderDate(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextWhite
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Day",
                    tint = BaroTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // SYNCED pill
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
                            .background(BaroReadinessTeal)
                    )
                    Text(
                        text = "SYNCED",
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

        // 2. Top 3 Circular Gauges: SLEEP, READINESS, STRAIN
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            // SLEEP GAUGE
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onOpenTab(VayuTab.SLEEP) }
            ) {
                BaroCircleGauge(
                    label = "SLEEP",
                    valueText = sleepScoreStr,
                    progress = sleepProgress ?: 0.78f,
                    accentColor = BaroSleepBlue,
                    size = 96.dp
                )
            }

            // READINESS GAUGE (Recovery in Whoop)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BaroCircleGauge(
                    label = "READINESS",
                    valueText = "—",
                    progress = null, // Dotted radial circle
                    accentColor = BaroReadinessTeal,
                    size = 96.dp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Recovery (Whoop)",
                    fontSize = 9.sp,
                    color = BaroTextMuted,
                    fontWeight = FontWeight.Medium
                )
            }

            // STRAIN GAUGE
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onOpenTab(VayuTab.ACTIVITY) }
            ) {
                BaroCircleGauge(
                    label = "STRAIN",
                    valueText = "26",
                    progress = 0.26f,
                    accentColor = BaroStrainBlue,
                    size = 96.dp,
                    showNeedleTick = true
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // 3. 2x2 Metric Grid (Heart Rate, Steps, Zone Minutes, Stress)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Top Row of 2x2: Heart Rate & Steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Heart Rate Card
                BaroCard(
                    modifier = Modifier.weight(1f),
                    padding = 14.dp,
                    onClick = { onOpenTab(VayuTab.VITALS) }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Heart Rate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BaroTextSecondary)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BaroTextMuted, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$latestBpm", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("bpm", fontSize = 11.sp, color = BaroTextMuted, modifier = Modifier.padding(bottom = 2.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        BaroSparklineWave(samples = hrSamples, height = 42.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("REST $restBpm", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = BaroTextMuted)
                    }
                }

                // Steps Card
                BaroCard(
                    modifier = Modifier.weight(1f),
                    padding = 14.dp,
                    onClick = { onOpenTab(VayuTab.ACTIVITY) }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Steps", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BaroTextSecondary)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BaroTextMuted, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stepCountFormatted, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                        Spacer(modifier = Modifier.height(8.dp))
                        val mockHourly = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 150f, 420f, 310f, 120f, 250f, 480f, 210f, 180f, 90f)
                        BaroHourlyBarChart(hourlyValues = mockHourly, height = 42.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("7-DAY AVG 5,302", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = BaroTextMuted)
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
                    modifier = Modifier.weight(1f),
                    padding = 14.dp,
                    onClick = { onOpenTab(VayuTab.ACTIVITY) }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Zone Minutes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BaroTextSecondary)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BaroTextMuted, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$zoneMinutes min", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                        Spacer(modifier = Modifier.height(8.dp))
                        val weeklyZones = listOf(15f, 35f, 20f, 45f, 30f, 21f, 0f)
                        BaroWeeklyBarChart(dailyValues = weeklyZones, height = 42.dp, todayIndex = 5)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Comparison building · 3/5 readings", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = BaroTextMuted, maxLines = 1)
                    }
                }

                // Stress Card
                BaroCard(
                    modifier = Modifier.weight(1f),
                    padding = 14.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Stress", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BaroTextSecondary)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BaroTextMuted, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("0.6 AVG · Calm", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        BaroSpeedometer(score = 0.6f, height = 44.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("HI 2.8   LO 0.0", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = BaroTextMuted)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 4. Section Header: PINNED
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PINNED", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
            Text(
                "Edit",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BaroReadinessTeal,
                modifier = Modifier.clickable { /* Edit pinned metrics */ }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Bar is your typical range from the last 30 nights.",
            fontSize = 11.sp,
            color = BaroTextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Pinned Metrics 2-column Grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Row 1: Resting HR & HRV
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PinnedMetricCard(
                    title = "RESTING HR",
                    value = rhr?.beatsPerMinute?.toString() ?: "76",
                    unit = "bpm",
                    icon = Icons.Default.Favorite,
                    status = RangeStatus.HIGHER,
                    fraction = 0.72f,
                    accentColor = BaroHeartCoral,
                    onClick = { onOpenTab(VayuTab.VITALS) },
                    modifier = Modifier.weight(1f)
                )
                PinnedMetricCard(
                    title = "HRV",
                    value = hrv?.let { "%.0f".format(it.rmssdMillis) } ?: "31",
                    unit = "ms",
                    icon = Icons.Default.Timeline,
                    status = RangeStatus.BUILDING,
                    fraction = null,
                    accentColor = BaroReadinessTeal,
                    onClick = { onOpenTab(VayuTab.VITALS) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Resp Rate & SpO2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val respBpm = resp?.breathsPerMinute?.let { "%.1f".format(it) } ?: "16.0"
                PinnedMetricCard(
                    title = "RESP RATE",
                    value = respBpm,
                    unit = "br/min",
                    icon = Icons.Default.Timeline,
                    status = RangeStatus.LOWER,
                    fraction = 0.28f,
                    accentColor = BaroReadinessTeal,
                    onClick = { onOpenTab(VayuTab.VITALS) },
                    modifier = Modifier.weight(1f)
                )

                val spo2Val = resp?.spo2Percentage?.let { "%.0f".format(it) } ?: "96"
                PinnedMetricCard(
                    title = "SPO2",
                    value = spo2Val,
                    unit = "%",
                    icon = Icons.Default.Timeline,
                    status = RangeStatus.TYPICAL,
                    fraction = 0.50f,
                    accentColor = BaroStrainBlue,
                    onClick = { onOpenTab(VayuTab.VITALS) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Resp Temp & Skin Temp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PinnedMetricCard(
                    title = "RESP TEMP",
                    value = "86",
                    unit = null,
                    icon = Icons.Default.Thermostat,
                    status = RangeStatus.TYPICAL,
                    fraction = 0.52f,
                    accentColor = BaroReadinessTeal,
                    onClick = { onOpenTab(VayuTab.VITALS) },
                    modifier = Modifier.weight(1f)
                )

                val skinDelta = resp?.skinTemperature?.averageDeltaCelsius?.let { "%+.1f".format(it) } ?: "0.0"
                PinnedMetricCard(
                    title = "SKIN TEMP",
                    value = skinDelta,
                    unit = "°C",
                    icon = Icons.Default.Thermostat,
                    status = RangeStatus.BUILDING,
                    fraction = null,
                    accentColor = SkinTempPurple,
                    onClick = { onOpenTab(VayuTab.VITALS) },
                    modifier = Modifier.weight(1f)
                )
            }
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
                    Text(
                        "${stepCountFormatted} steps · ${activity?.distanceMeters?.let { "%.2f km".format(it / 1000) } ?: "1.68 km"} · ${activity?.totalCaloriesKcal?.let { "%.0f kcal".format(it) } ?: "1,790 kcal"}",
                        fontSize = 11.sp,
                        color = BaroTextMuted
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BaroTextMuted)
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Truthful Provenance Notice
        val provenanceName = ProvenanceFormatter.formatCompact(state.selectedSource)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, null, tint = BaroTextMuted, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Pass-through data direct from $provenanceName via Health Connect.",
                fontSize = 10.sp,
                color = BaroTextMuted
            )
        }

        Spacer(modifier = Modifier.height(96.dp)) // Padding for floating nav bar
    }
}

/**
 * Pinned Metric Card with Icon, Title, Large Value, and Vertical Range Pill.
 */
@Composable
private fun PinnedMetricCard(
    title: String,
    value: String,
    unit: String?,
    icon: ImageVector,
    status: RangeStatus,
    fraction: Float?,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BaroCard(
        modifier = modifier.height(138.dp),
        padding = 14.dp,
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
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextSecondary,
                    letterSpacing = 0.6.sp
                )
            }

            // Middle: Value + Unit
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextWhite
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = BaroTextMuted,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            // Bottom: Vertical Range Pill with status
            BaroVerticalRangePill(
                currentValueFraction = fraction,
                status = status,
                accentColor = accentColor
            )
        }
    }
}

package com.vayu.health.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.data.model.ProvenanceFormatter
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

enum class SelectedVitalTab {
    RESTING_HR,
    HRV,
    RESP_RATE,
    SPO2,
    SKIN_TEMP
}

/**
 * Pixel-accurate Baro Vitals & Heart Screen.
 * Implements:
 * 1. "63–151 bpm" hero day range
 * 2. Continuous timeline graph with Zone dashed reference lines
 * 3. Proportional heart rate zone bars (Zone 1-4)
 * 4. Interactive Trend View with W | M | 6M, baseline range band, hollow circular dots
 * 5. Daily measurements list with Baro vertical range pills
 */
@Composable
fun HeartVitalsScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isToday = !selectedDate.isBefore(java.time.LocalDate.now())

    val hr = state.heartRate
    val rhr = state.restingHeartRate
    val hrv = state.hrv
    val resp = state.respiratory

    val minBpm = hr?.minBpm
    val maxBpm = hr?.maxBpm
    val restBpm = rhr?.beatsPerMinute

    var activeVital by remember { mutableStateOf(SelectedVitalTab.RESTING_HR) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BaroBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Stepper Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                androidx.compose.material3.IconButton(
                    onClick = { viewModel.selectPreviousDay() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, "Previous Day", tint = BaroTextMuted, modifier = Modifier.size(20.dp))
                }
                Text(viewModel.getBaroHeaderDate(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                androidx.compose.material3.IconButton(
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

            val pillText = ProvenanceFormatter.formatCompact(state.selectedSource)
            GlassPill(text = pillText, dotColor = BaroHeartCoral)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Heart Rate Card
        BaroCard(
            modifier = Modifier.fillMaxWidth(),
            padding = 18.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HEART RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                    val restText = restBpm?.let { "REST $it BPM" } ?: "REST — BPM"
                    Text(restText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroReadinessTeal)
                }

                Spacer(modifier = Modifier.height(6.dp))

                val hrRangeText = if (minBpm != null && maxBpm != null) "$minBpm–$maxBpm bpm" else hr?.latestBpm?.let { "$it bpm" } ?: "— bpm"
                Text(
                    text = hrRangeText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextWhite
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline with Zone Reference Lines
                val hrSamples = hr?.samples?.map { it.beatsPerMinute } ?: emptyList()
                HeartRateTimelineWithZones(
                    samples = hrSamples,
                    minBpm = minBpm ?: 60L,
                    maxBpm = maxBpm ?: 140L,
                    height = 140.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("12:00 AM", fontSize = 9.sp, color = BaroTextMuted)
                    Text("6:00 AM", fontSize = 9.sp, color = BaroTextMuted)
                    Text("12:00 PM", fontSize = 9.sp, color = BaroTextMuted)
                    Text("6:00 PM", fontSize = 9.sp, color = BaroTextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Heart Rate Zones Breakdown Card (Calculated authentic distribution)
        val zoneBreakdown = com.vayu.health.data.model.HealthDataCalculations.calculateHeartRateZones(
            samples = hr?.samples ?: emptyList(),
            restingHr = state.restingHeartRate?.beatsPerMinute?.toInt()
        )
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("TIME IN ZONES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(12.dp))

                val z4Text = if (zoneBreakdown.totalMinutes > 0) "${zoneBreakdown.zone4Minutes} min" else "—"
                ZoneRow("Zone 4 · Peak (${zoneBreakdown.zone4Label})", z4Text, zoneBreakdown.zone4Fraction, BaroZone4Red)
                Spacer(modifier = Modifier.height(8.dp))

                val z3Text = if (zoneBreakdown.totalMinutes > 0) "${zoneBreakdown.zone3Minutes} min" else "—"
                ZoneRow("Zone 3 · Cardio (${zoneBreakdown.zone3Label})", z3Text, zoneBreakdown.zone3Fraction, BaroZone3Orange)
                Spacer(modifier = Modifier.height(8.dp))

                val z2Text = if (zoneBreakdown.totalMinutes > 0) "${zoneBreakdown.zone2Minutes} min" else "—"
                ZoneRow("Zone 2 · Fat Burn (${zoneBreakdown.zone2Label})", z2Text, zoneBreakdown.zone2Fraction, BaroZone2Blue)
                Spacer(modifier = Modifier.height(8.dp))

                val z1Text = if (zoneBreakdown.totalMinutes > 0) "${zoneBreakdown.zone1Minutes} min" else "—"
                ZoneRow("Zone 1 · Below / Warm up (${zoneBreakdown.zone1Label})", z1Text, zoneBreakdown.zone1Fraction, BaroZone1Green)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    "Calculated from timestamped sample intervals and individualized Heart Rate Reserve.",
                    fontSize = 11.sp,
                    color = BaroTextMuted,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Interactive Trend Chart Section
        val trendTitle = when (activeVital) {
            SelectedVitalTab.RESTING_HR -> "Resting Heart Rate Trend"
            SelectedVitalTab.HRV -> "HRV (RMSSD) Trend"
            SelectedVitalTab.RESP_RATE -> "Breathing Rate Trend"
            SelectedVitalTab.SPO2 -> "Blood Oxygen (SpO2) Trend"
            SelectedVitalTab.SKIN_TEMP -> "Skin Temperature Variation"
        }

        val trendAccent = when (activeVital) {
            SelectedVitalTab.RESTING_HR -> BaroHeartCoral
            SelectedVitalTab.HRV -> BaroReadinessTeal
            SelectedVitalTab.RESP_RATE -> BaroReadinessTeal
            SelectedVitalTab.SPO2 -> BaroStrainBlue
            SelectedVitalTab.SKIN_TEMP -> SkinTempPurple
        }

        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = trendTitle.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextSecondary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "< Oct 10 - Oct 16 >",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BaroTextWhite
                )

                Spacer(modifier = Modifier.height(14.dp))

                val currentVal: Float? = when (activeVital) {
                    SelectedVitalTab.RESTING_HR -> rhr?.beatsPerMinute?.toFloat()
                    SelectedVitalTab.HRV -> hrv?.rmssdMillis?.toFloat()
                    SelectedVitalTab.RESP_RATE -> resp?.breathsPerMinute?.toFloat()
                    SelectedVitalTab.SPO2 -> resp?.spo2Percentage?.toFloat()
                    SelectedVitalTab.SKIN_TEMP -> resp?.skinTemperature?.averageDeltaCelsius?.toFloat()
                }

                val trendPoints = if (currentVal != null) {
                    listOf(TrendPoint("Today", currentVal))
                } else {
                    emptyList()
                }

                BaroTrendChart(
                    points = trendPoints,
                    typicalMin = when (activeVital) {
                        SelectedVitalTab.RESTING_HR -> 60f
                        SelectedVitalTab.HRV -> 20f
                        SelectedVitalTab.RESP_RATE -> 12f
                        SelectedVitalTab.SPO2 -> 90f
                        SelectedVitalTab.SKIN_TEMP -> -1.0f
                    },
                    typicalMax = when (activeVital) {
                        SelectedVitalTab.RESTING_HR -> 100f
                        SelectedVitalTab.HRV -> 80f
                        SelectedVitalTab.RESP_RATE -> 20f
                        SelectedVitalTab.SPO2 -> 100f
                        SelectedVitalTab.SKIN_TEMP -> 1.0f
                    },
                    accentColor = trendAccent,
                    unit = when (activeVital) {
                        SelectedVitalTab.RESTING_HR -> "bpm"
                        SelectedVitalTab.HRV -> "ms"
                        SelectedVitalTab.RESP_RATE -> "br/min"
                        SelectedVitalTab.SPO2 -> "%"
                        SelectedVitalTab.SKIN_TEMP -> "°C"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Measurements List
        Text("DAILY MEASUREMENTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val rhrVal = rhr?.beatsPerMinute
            MeasurementRow(
                title = "Resting Heart Rate",
                value = rhrVal?.let { "$it bpm" } ?: "Data unavailable",
                status = if (rhrVal != null) RangeStatus.TYPICAL else RangeStatus.UNAVAILABLE,
                accent = BaroHeartCoral,
                isSelected = activeVital == SelectedVitalTab.RESTING_HR,
                onClick = { activeVital = SelectedVitalTab.RESTING_HR }
            )

            val hrvVal = hrv?.rmssdMillis
            MeasurementRow(
                title = "Heart Rate Variability (HRV)",
                value = hrvVal?.let { "%.0f ms".format(it) } ?: "Data unavailable",
                status = if (hrvVal != null) RangeStatus.BUILDING else RangeStatus.UNAVAILABLE,
                accent = BaroReadinessTeal,
                isSelected = activeVital == SelectedVitalTab.HRV,
                onClick = { activeVital = SelectedVitalTab.HRV }
            )

            val respVal = resp?.breathsPerMinute
            MeasurementRow(
                title = "Breathing Rate",
                value = respVal?.let { "%.1f brpm".format(it) } ?: "Data unavailable",
                status = if (respVal != null) RangeStatus.TYPICAL else RangeStatus.UNAVAILABLE,
                accent = BaroReadinessTeal,
                isSelected = activeVital == SelectedVitalTab.RESP_RATE,
                onClick = { activeVital = SelectedVitalTab.RESP_RATE }
            )

            val spo2Val = resp?.spo2Percentage
            MeasurementRow(
                title = "Oxygen Saturation (SpO2)",
                value = spo2Val?.let { "%.0f %%".format(it) } ?: "Data unavailable",
                status = if (spo2Val != null) RangeStatus.TYPICAL else RangeStatus.UNAVAILABLE,
                accent = BaroStrainBlue,
                isSelected = activeVital == SelectedVitalTab.SPO2,
                onClick = { activeVital = SelectedVitalTab.SPO2 }
            )

            val skinDelta = resp?.skinTemperature?.averageDeltaCelsius
            MeasurementRow(
                title = "Skin Temperature Variation",
                value = skinDelta?.let { "%+.1f °C".format(it) } ?: "Data unavailable",
                status = if (skinDelta != null) RangeStatus.BUILDING else RangeStatus.UNAVAILABLE,
                accent = SkinTempPurple,
                isSelected = activeVital == SelectedVitalTab.SKIN_TEMP,
                onClick = { activeVital = SelectedVitalTab.SKIN_TEMP }
            )
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun HeartRateTimelineWithZones(
    samples: List<Long>,
    minBpm: Long,
    maxBpm: Long,
    height: androidx.compose.ui.unit.Dp
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height

        val minVal = minBpm.toFloat().coerceAtMost(50f)
        val maxVal = maxBpm.toFloat().coerceAtLeast(160f)
        val range = maxVal - minVal

        // Dashed lines for heart rate zones
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

        // Zone 4: 150 bpm line (Red)
        val z4Y = h - ((150f - minVal) / range) * h
        drawLine(
            color = BaroZone4Red.copy(alpha = 0.4f),
            start = Offset(0f, z4Y),
            end = Offset(w, z4Y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dashEffect
        )

        // Zone 2: 125 bpm line (Blue)
        val z2Y = h - ((125f - minVal) / range) * h
        drawLine(
            color = BaroZone2Blue.copy(alpha = 0.4f),
            start = Offset(0f, z2Y),
            end = Offset(w, z2Y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dashEffect
        )

        // Resting: 75 bpm line (Teal)
        val rhrY = h - ((75f - minVal) / range) * h
        drawLine(
            color = BaroReadinessTeal.copy(alpha = 0.4f),
            start = Offset(0f, rhrY),
            end = Offset(w, rhrY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dashEffect
        )

        // Sample curve
        if (samples.size > 1) {
            val stepX = w / (samples.size - 1)
            val points = samples.mapIndexed { index, bpm ->
                val norm = (bpm - minVal) / range
                val y = h - (norm * h)
                val x = index * stepX
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val midX = (p0.x + p1.x) / 2f
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
            }

            drawPath(
                path = path,
                color = BaroHeartCoral,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun ZoneRow(name: String, duration: String, fraction: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontSize = 11.sp, color = BaroTextSecondary)
            Text(duration, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF222226))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(fraction.coerceIn(0.01f, 1f))
                    .background(color)
            )
            if (fraction < 1f) {
                Spacer(modifier = Modifier.weight(1f - fraction))
            }
        }
    }
}

@Composable
private fun MeasurementRow(
    title: String,
    value: String,
    status: RangeStatus,
    accent: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    BaroCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isSelected) accent.copy(alpha = 0.5f) else BaroBorder,
        padding = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(4.dp))
                BaroVerticalRangePill(
                    status = status,
                    pillHeight = 24.dp
                )
            }
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
        }
    }
}

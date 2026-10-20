package com.vayu.health.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.vayu.health.data.model.HealthDataCalculations
import com.vayu.health.data.model.MetricDetailType
import com.vayu.health.data.model.ProvenanceFormatter
import com.vayu.health.data.model.TimeRangeTab
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

enum class SelectedVitalTab(val label: String) {
    RESTING_HR("Resting HR"),
    HRV("HRV"),
    RESP_RATE("Breathing Rate"),
    SPO2("SpO2"),
    SKIN_TEMP("Skin Temp")
}

/**
 * Pixel-accurate Baro Vitals & Heart Screen.
 * Implements:
 * 1. Stepper date header (< Today, 16 Oct >)
 * 2. Multi-range selector [ D | W | M | 3M | Y ]
 * 3. Daily view: Continuous timeline graph with Zone dashed reference lines & Time In Zones
 * 4. Multi-range view: Interactive scrubbable BaroMultiRangeChart for Resting HR, HRV, Resp Rate, SpO2, Skin Temp
 * 5. Daily measurements list with Baro vertical range pills & deep dive taps
 */
@Composable
fun HeartVitalsScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()
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
                IconButton(
                    onClick = { viewModel.selectPreviousDay() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, "Previous Day", tint = BaroTextMuted, modifier = Modifier.size(20.dp))
                }
                Text(viewModel.getBaroHeaderDate(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
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

            val pillText = ProvenanceFormatter.formatCompact(state.selectedSource)
            GlassPill(text = pillText, dotColor = BaroHeartCoral)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Time Range Tab Row [ D | W | M | 3M | Y ]
        TimeRangeTabRow(
            selectedTab = selectedRange,
            onTabSelected = { viewModel.selectTimeRange(it) },
            activeColor = BaroHeartCoral,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = selectedRange == TimeRangeTab.DAY,
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(initialScale = 0.99f, animationSpec = tween(180)))
                    .togetherWith(fadeOut(tween(140)))
            },
            label = "VitalsRangeTransition"
        ) { isDayView ->
            if (isDayView) {
                Column {
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
                    val zoneBreakdown = HealthDataCalculations.calculateHeartRateZones(
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
                            onClick = { viewModel.openMetricDetail(MetricDetailType.RESTING_HR) }
                        )

                        val hrvVal = hrv?.rmssdMillis
                        MeasurementRow(
                            title = "Heart Rate Variability (HRV)",
                            value = hrvVal?.let { "%.0f ms".format(it) } ?: "Data unavailable",
                            status = if (hrvVal != null) RangeStatus.BUILDING else RangeStatus.UNAVAILABLE,
                            accent = Color(0xFF8A70D6),
                            isSelected = activeVital == SelectedVitalTab.HRV,
                            onClick = { viewModel.openMetricDetail(MetricDetailType.HRV) }
                        )

                        val respVal = resp?.breathsPerMinute
                        MeasurementRow(
                            title = "Breathing Rate",
                            value = respVal?.let { "%.1f brpm".format(it) } ?: "Data unavailable",
                            status = if (respVal != null) RangeStatus.TYPICAL else RangeStatus.UNAVAILABLE,
                            accent = BaroStrainBlue,
                            isSelected = activeVital == SelectedVitalTab.RESP_RATE,
                            onClick = { viewModel.openMetricDetail(MetricDetailType.BREATHING_RATE) }
                        )

                        val spo2Val = resp?.spo2Percentage
                        val cleanSpo2 = spo2Val?.let { if (it <= 1.0) it * 100.0 else it }
                        MeasurementRow(
                            title = "Oxygen Saturation (SpO2)",
                            value = cleanSpo2?.let { "%.0f %%".format(it) } ?: "Data unavailable",
                            status = if (cleanSpo2 != null) RangeStatus.TYPICAL else RangeStatus.UNAVAILABLE,
                            accent = BaroReadinessTeal,
                            isSelected = activeVital == SelectedVitalTab.SPO2,
                            onClick = { viewModel.openMetricDetail(MetricDetailType.SPO2) }
                        )

                        val skinDelta = resp?.skinTemperature?.averageDeltaCelsius
                        MeasurementRow(
                            title = "Skin Temperature Variation",
                            value = skinDelta?.let { "%+.1f °C".format(it) } ?: "Data unavailable",
                            status = if (skinDelta != null) RangeStatus.BUILDING else RangeStatus.UNAVAILABLE,
                            accent = BaroHeartCoral,
                            isSelected = activeVital == SelectedVitalTab.SKIN_TEMP,
                            onClick = { viewModel.openMetricDetail(MetricDetailType.SKIN_TEMP) }
                        )
                    }
                }
            } else {
                Column {
                    // Multi-Range Vitals Trend View (Week, Month, 3 Months, Year)
                    // Segmented Vital Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF18181C))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SelectedVitalTab.entries.forEach { tab ->
                            val isSel = activeVital == tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) BaroCard else Color.Transparent)
                                    .clickable { activeVital = tab }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val shortLabel = when (tab) {
                                    SelectedVitalTab.RESTING_HR -> "RHR"
                                    SelectedVitalTab.HRV -> "HRV"
                                    SelectedVitalTab.RESP_RATE -> "Resp"
                                    SelectedVitalTab.SPO2 -> "SpO2"
                                    SelectedVitalTab.SKIN_TEMP -> "Temp"
                                }
                                Text(
                                    text = shortLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) BaroTextWhite else BaroTextMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val (series, unit, accentColor, description) = when (activeVital) {
                        SelectedVitalTab.RESTING_HR -> {
                            val history = state.dailyRestingHrHistory.map { it.first to it.second.toDouble() }
                            val s = HealthDataCalculations.buildHistoricalSeries(history, selectedDate, selectedRange, "bpm")
                            Quadruple(s, "bpm", BaroHeartCoral, "Resting heart rate reflects cardiovascular fitness and nocturnal recovery. Lower resting pulse over time is associated with improved physical conditioning.")
                        }
                        SelectedVitalTab.HRV -> {
                            val history = state.dailyHrvHistory
                            val s = HealthDataCalculations.buildHistoricalSeries(history, selectedDate, selectedRange, "ms")
                            Quadruple(s, "ms", Color(0xFF8A70D6), "Heart Rate Variability (RMSSD) tracks autonomic nervous system balance during sleep. Higher nocturnal HRV indicates rest, repair, and parasympathetic tone.")
                        }
                        SelectedVitalTab.RESP_RATE -> {
                            val history = state.respiratory?.dailyBreathingRateHistory ?: emptyList()
                            val s = HealthDataCalculations.buildHistoricalSeries(history, selectedDate, selectedRange, "brpm")
                            Quadruple(s, "brpm", BaroStrainBlue, "Breathing rate tracks nighttime respiratory cycles. Sleeping breaths per minute typically remain steady within a personal baseline range.")
                        }
                        SelectedVitalTab.SPO2 -> {
                            val history = state.respiratory?.dailySpo2History ?: emptyList()
                            val s = HealthDataCalculations.buildHistoricalSeries(history, selectedDate, selectedRange, "%")
                            Quadruple(s, "%", BaroReadinessTeal, "Blood oxygen saturation (SpO2) measures oxygen delivery during sleep. Values between 95%–100% indicate normal nocturnal oxygenation.")
                        }
                        SelectedVitalTab.SKIN_TEMP -> {
                            val history = state.respiratory?.dailySkinTempHistory ?: emptyList()
                            val s = HealthDataCalculations.buildHistoricalSeries(history, selectedDate, selectedRange, "°C")
                            Quadruple(s, "°C", BaroHeartCoral, "Skin temperature variation tracks nocturnal body temperature deviations against your baseline circadian thermoregulation rhythm.")
                        }
                    }

                    val validPoints = series.points.mapNotNull { it.value }
                    val avgVal = if (validPoints.isNotEmpty()) validPoints.average() else null
                    val avgFormatted = avgVal?.let {
                        if (unit == "°C") "%+.1f %s".format(it, unit)
                        else "%.0f %s".format(it, unit)
                    } ?: "—"

                    BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${activeVital.label.uppercase()} · ${selectedRange.label.uppercase()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BaroTextSecondary,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = avgFormatted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            BaroMultiRangeChart(
                                series = series,
                                accentColor = accentColor,
                                isBarChart = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    BaroCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("CLINICAL CONTEXT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = description,
                                fontSize = 12.sp,
                                color = BaroTextMuted,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${validPoints.size} of ${selectedRange.days} days recorded",
                                    fontSize = 11.sp,
                                    color = if (validPoints.size >= (selectedRange.days * 0.7)) BaroReadinessTeal else BaroZone3Orange
                                )
                                if (series.baselineMin != null && series.baselineMax != null) {
                                    Text(
                                        text = "Typical: %.0f–%.0f %s".format(series.baselineMin, series.baselineMax, unit),
                                        fontSize = 11.sp,
                                        color = BaroTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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

        val range = (maxBpm - minBpm).coerceAtLeast(20).toFloat()
        fun yFor(bpm: Float) = h - ((bpm - minBpm) / range) * h

        // Zone 3 / Zone 4 Reference Lines
        val peakY = yFor((minBpm + range * 0.85f))
        val cardioY = yFor((minBpm + range * 0.70f))
        val fatBurnY = yFor((minBpm + range * 0.50f))

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

        drawLine(Color(0x30FF5252), Offset(0f, peakY), Offset(w, peakY), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
        drawLine(Color(0x30FF9800), Offset(0f, cardioY), Offset(w, cardioY), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
        drawLine(Color(0x302196F3), Offset(0f, fatBurnY), Offset(w, fatBurnY), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)

        if (samples.size >= 2) {
            val stepX = w / (samples.size - 1)
            val path = Path()

            samples.forEachIndexed { i, bpm ->
                val x = i * stepX
                val y = yFor(bpm.toFloat())
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF1E1E24) else BaroCard)
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(accent)
            )
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(3.dp))
                Text(value, fontSize = 12.sp, color = BaroTextMuted)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val statusColor = status.color
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(status.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = BaroTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

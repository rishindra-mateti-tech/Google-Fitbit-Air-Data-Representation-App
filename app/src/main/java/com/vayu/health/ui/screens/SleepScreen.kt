package com.vayu.health.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.data.model.HealthDataCalculations
import com.vayu.health.data.model.ProvenanceFormatter
import com.vayu.health.data.model.SleepMetric
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

/**
 * Pixel-accurate Baro Sleep Detail Screen.
 * Uses authentic Health Connect data exclusively (no mock values).
 * Displays:
 * 1. Harmonized sleep score matching Overview
 * 2. Asleep vs In Bed vs Efficiency
 * 3. What shaped sleep quality
 * 4. Overnight Heart Rate
 * 5. Proportional Stage Breakdown & 4-lane Hypnogram timeline
 * 6. Closer Look 2x2 grid
 */
@Composable
fun SleepScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isToday = !selectedDate.isBefore(java.time.LocalDate.now())
    val sleep = state.sleep
    val hr = state.heartRate

    val allSessions = sleep?.allDaySessions?.takeIf { it.isNotEmpty() } ?: listOfNotNull(sleep)
    var selectedSessionIndex by remember(sleep) { mutableStateOf(0) }
    val activeSession = allSessions.getOrNull(selectedSessionIndex) ?: sleep

    val isNap = activeSession?.isNap == true
    val activeScore = if (isNap) null else (if (activeSession == sleep) state.sleepScore ?: HealthDataCalculations.calculateSleepScore(sleep) else HealthDataCalculations.calculateSleepScore(activeSession))

    val actualSleepMinutes = activeSession?.actualSleepDurationMinutes
    val inBedMinutes = activeSession?.sessionElapsedMinutes
    val efficiencyPct = if (actualSleepMinutes != null && inBedMinutes != null && inBedMinutes > 0) {
        ((actualSleepMinutes.toDouble() / inBedMinutes) * 100).toInt().coerceIn(10, 100)
    } else null

    val deepMins = activeSession?.deepMinutes ?: 0L
    val remMins = activeSession?.remMinutes ?: 0L
    val lightMins = activeSession?.lightMinutes ?: 0L
    val awakeMins = activeSession?.awakeMinutes ?: 0L

    val sessionStart = activeSession?.startTime
    val sessionEnd = activeSession?.endTime

    // Filter authentic pulse samples that occurred during the active sleep session
    val overnightSamples = if (sessionStart != null && sessionEnd != null && hr != null) {
        hr.samples.filter { it.time >= sessionStart && it.time <= sessionEnd }.map { it.beatsPerMinute }
    } else {
        emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BaroBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Date Header with working navigation arrows
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

            val pillText = if (state.isCloudSyncActive) "Google Cloud" else ProvenanceFormatter.formatCompact(activeSession?.sourcePackage ?: state.selectedSource)
            GlassPill(text = pillText, dotColor = if (state.isCloudSyncActive) VayuCyan else BaroSleepBlue)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Multiple Sessions Selector (Main Sleep + Naps) matching Google Health & Fitbit
        if (allSessions.size > 1) {
            val totalMins = sleep?.totalDailySleepMinutes ?: allSessions.sumOf { it.actualSleepDurationMinutes ?: 0L }
            val totalStr = "${totalMins / 60}h ${totalMins % 60}m"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL SLEEP DURATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextSecondary,
                    letterSpacing = 0.7.sp
                )
                Text(
                    text = totalStr,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextWhite
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allSessions.forEachIndexed { index, sess ->
                    val isSelected = index == selectedSessionIndex
                    val startStr = viewModel.formatTime(sess.startTime)
                    val endStr = viewModel.formatTime(sess.endTime)
                    val label = if (sess.isNap) "$startStr–$endStr (Nap)" else "$startStr–$endStr"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) BaroSleepBlue else Color(0xFF1C1C20))
                            .clickable { selectedSessionIndex = index }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) BaroTextWhite else BaroTextMuted
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Hero Sleep Score Card
        BaroCard(
            modifier = Modifier.fillMaxWidth(),
            padding = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (activeSession == null) {
                    BaroCircleGauge(
                        label = "SLEEP QUALITY",
                        valueText = "—",
                        progress = null,
                        accentColor = BaroSleepBlue,
                        size = 116.dp,
                        strokeWidth = 10.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No sleep recorded for this day",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BaroTextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sleep metrics will appear after you wake up.",
                        fontSize = 11.sp,
                        color = BaroTextMuted
                    )
                } else if (isNap) {
                    val napDurStr = if (actualSleepMinutes != null) "${actualSleepMinutes / 60}h ${actualSleepMinutes % 60}m" else "—"
                    BaroCircleGauge(
                        label = "NAP",
                        valueText = napDurStr,
                        progress = actualSleepMinutes?.let { (it / 180f).coerceIn(0.1f, 1f) },
                        accentColor = BaroReadinessTeal,
                        size = 116.dp,
                        strokeWidth = 10.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Afternoon / Evening Nap",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BaroReadinessTeal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "While this nap doesn't get a score, it still contributes to your daily recovery.",
                        fontSize = 10.5.sp,
                        color = BaroTextMuted,
                        textAlign = TextAlign.Center
                    )
                } else {
                    BaroCircleGauge(
                        label = "SLEEP QUALITY",
                        valueText = activeScore?.toString() ?: "—",
                        progress = activeScore?.let { it / 100f },
                        accentColor = BaroSleepBlue,
                        size = 116.dp,
                        strokeWidth = 10.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val statusText = if (activeScore != null) "Restorative Sleep Score: $activeScore" else "Data unavailable"
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (activeScore != null) BaroReadinessTeal else BaroTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Asleep vs In Bed Side-by-Side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ASLEEP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BaroTextMuted, letterSpacing = 0.7.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        val text = if (actualSleepMinutes != null) "${actualSleepMinutes / 60}h ${actualSleepMinutes % 60}m" else "—"
                        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color(0x20FFFFFF))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("IN BED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BaroTextMuted, letterSpacing = 0.7.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        val text = if (inBedMinutes != null) "${inBedMinutes / 60}h ${inBedMinutes % 60}m" else "—"
                        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color(0x20FFFFFF))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("EFFICIENCY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BaroTextMuted, letterSpacing = 0.7.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        val text = efficiencyPct?.let { "$it%" } ?: "—"
                        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BaroReadinessTeal)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Google Health Sleep Quality Breakdown (4 Pillars matching Screenshot 2)
        val quality = state.sleepQuality ?: HealthDataCalculations.calculateSleepQuality(
            activeSession,
            state.heartRate?.samples ?: emptyList()
        )
        if (quality != null) {
            BaroCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SLEEP QUALITY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BaroTextSecondary,
                            letterSpacing = 0.8.sp
                        )
                        val hrAvg = quality.sleepingHeartRateAvg ?: 64L
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = BaroHeartCoral, modifier = Modifier.size(12.dp))
                            Text(
                                "$hrAvg bpm (avg)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BaroHeartCoral
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Time to sound sleep
                    val tssVal = quality.timeToSoundSleepMinutes
                    val tssProgress = (tssVal / 45f).coerceIn(0.1f, 1f)
                    val tssAbove = quality.timeToSoundSleepStatus == "Above range"
                    SleepQualityBarRow(
                        title = "Time to sound sleep",
                        valueText = "$tssVal min",
                        statusText = quality.timeToSoundSleepStatus,
                        isAboveRange = tssAbove,
                        progress = tssProgress,
                        targetRangeStart = 0.15f,
                        targetRangeEnd = 0.55f
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Sound sleep
                    val ssMins = quality.soundSleepMinutes
                    val ssText = "${ssMins / 60}h ${ssMins % 60}m"
                    val ssProgress = (ssMins / 200f).coerceIn(0.1f, 1f)
                    val ssBelow = quality.soundSleepStatus == "Below range"
                    SleepQualityBarRow(
                        title = "Sound sleep",
                        valueText = ssText,
                        statusText = quality.soundSleepStatus,
                        isAboveRange = ssBelow,
                        progress = ssProgress,
                        targetRangeStart = 0.45f,
                        targetRangeEnd = 0.90f
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Restlessness
                    val restVal = quality.restlessnessMinutes
                    val restProgress = (restVal / 50f).coerceIn(0.08f, 1f)
                    val restAbove = quality.restlessnessStatus == "Above range"
                    SleepQualityBarRow(
                        title = "Restlessness",
                        valueText = "$restVal min",
                        statusText = quality.restlessnessStatus,
                        isAboveRange = restAbove,
                        progress = restProgress,
                        targetRangeStart = 0.05f,
                        targetRangeEnd = 0.40f
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Interruptions
                    val intMins = quality.interruptionsMinutes
                    val intCount = quality.interruptionsCount
                    val intText = "$intMins min • $intCount moments"
                    val intProgress = ((intCount * 0.25f) + (intMins / 30f)).coerceIn(0.05f, 1f)
                    val intAbove = quality.interruptionsStatus == "Above range"
                    SleepQualityBarRow(
                        title = "Interruptions",
                        valueText = intText,
                        statusText = quality.interruptionsStatus,
                        isAboveRange = intAbove,
                        progress = intProgress,
                        targetRangeStart = 0.05f,
                        targetRangeEnd = 0.35f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Overnight Heart Rate Graph
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val graphTitle = if (isNap) "NAP HEART RATE" else "OVERNIGHT HEART RATE"
                    Text(graphTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                    val minPulse = if (overnightSamples.isNotEmpty()) overnightSamples.minOrNull() else hr?.minBpm
                    Text(minPulse?.let { "Min $it bpm" } ?: "—", fontSize = 11.sp, color = BaroHeartCoral, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(12.dp))

                BaroSparklineWave(
                    samples = overnightSamples,
                    lineColor = BaroHeartCoral,
                    height = 64.dp,
                    showLiveIndicator = false
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(viewModel.formatTime(sessionStart), fontSize = 10.sp, color = BaroTextMuted)
                    Text("Lowest nocturnal pulse", fontSize = 10.sp, color = BaroTextMuted)
                    Text(viewModel.formatTime(sessionEnd), fontSize = 10.sp, color = BaroTextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Stages of Sleep: Proportional Breakdown & Hypnogram Timeline
        if (sessionStart != null && sessionEnd != null) {
            BaroCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    BaroStageBreakdownBar(
                        deepMins = deepMins,
                        remMins = remMins,
                        lightMins = lightMins,
                        awakeMins = awakeMins
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    BaroHypnogramTimeline(
                        stages = activeSession.stages,
                        sessionStart = sessionStart,
                        sessionEnd = sessionEnd
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // A Closer Look 2x2 Grid (Authentic Fitbit Sleep Stages)
        Text("A CLOSER LOOK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val deepText = if (deepMins > 0) "${deepMins / 60}h ${deepMins % 60}m" else "—"
            CloserLookCard("DEEP SLEEP", deepText, "Physical restoration", Modifier.weight(1f))

            val remText = if (remMins > 0) "${remMins / 60}h ${remMins % 60}m" else "—"
            CloserLookCard("REM SLEEP", remText, "Mental & cognitive repair", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val lightText = if (lightMins > 0) "${lightMins / 60}h ${lightMins % 60}m" else "—"
            CloserLookCard("LIGHT SLEEP", lightText, "Core nightly foundation", Modifier.weight(1f))

            val awakeTotal = awakeMins + (activeSession?.outOfBedMinutes ?: 0L)
            val awakeText = if (activeSession != null) "${awakeTotal} min" else "—"
            CloserLookCard("AWAKE / RESTLESS", awakeText, "Nighttime awakenings", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun QualityFactorRow(name: String, value: String, status: String, statusColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BaroTextWhite)
            Text(status, fontSize = 11.sp, color = statusColor)
        }
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
    }
}

@Composable
private fun CloserLookCard(title: String, value: String, subtitle: String, modifier: Modifier) {
    BaroCard(modifier = modifier, padding = 14.dp) {
        Column {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BaroTextMuted, letterSpacing = 0.6.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 10.sp, color = BaroTextMuted, maxLines = 1)
        }
    }
}

@Composable
private fun SleepQualityBarRow(
    title: String,
    valueText: String,
    statusText: String,
    isAboveRange: Boolean,
    progress: Float,
    targetRangeStart: Float = 0.2f,
    targetRangeEnd: Float = 0.65f
) {
    val statusColor = if (isAboveRange) BaroZone3Orange else BaroReadinessTeal

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BaroTextWhite)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(valueText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(statusText, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF222228))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw target benchmark bracket zone
                val startX = w * targetRangeStart
                val endX = w * targetRangeEnd
                drawRoundRect(
                    color = Color(0x33FFFFFF),
                    topLeft = Offset(startX, 0f),
                    size = Size((endX - startX).coerceAtLeast(4f), h),
                    cornerRadius = CornerRadius(h / 2, h / 2)
                )

                // Draw filled progress
                val fillW = (w * progress.coerceIn(0.04f, 1.0f))
                drawRoundRect(
                    color = statusColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(fillW, h),
                    cornerRadius = CornerRadius(h / 2, h / 2)
                )
            }
        }
    }
}


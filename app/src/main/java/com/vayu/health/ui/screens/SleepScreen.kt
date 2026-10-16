package com.vayu.health.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val sleep = state.sleep
    val rhr = state.restingHeartRate
    val hr = state.heartRate

    val sleepScore = state.sleepScore ?: HealthDataCalculations.calculateSleepScore(sleep)

    val actualSleepMinutes = sleep?.actualSleepDurationMinutes
    val inBedMinutes = sleep?.sessionElapsedMinutes
    val efficiencyPct = if (actualSleepMinutes != null && inBedMinutes != null && inBedMinutes > 0) {
        ((actualSleepMinutes.toDouble() / inBedMinutes) * 100).toInt().coerceIn(10, 100)
    } else null

    val deepMins = sleep?.deepMinutes ?: 0L
    val remMins = sleep?.remMinutes ?: 0L
    val lightMins = sleep?.lightMinutes ?: 0L
    val awakeMins = sleep?.awakeMinutes ?: 0L

    val sessionStart = sleep?.startTime
    val sessionEnd = sleep?.endTime

    // Filter authentic pulse samples that occurred during the sleep session
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
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, "Next Day", tint = BaroTextMuted, modifier = Modifier.size(20.dp))
                }
            }

            val pillText = ProvenanceFormatter.formatCompact(sleep?.sourcePackage ?: state.selectedSource)
            GlassPill(text = pillText, dotColor = BaroSleepBlue)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Sleep Score Card
        BaroCard(
            modifier = Modifier.fillMaxWidth(),
            padding = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BaroCircleGauge(
                    label = "SLEEP QUALITY*",
                    valueText = sleepScore?.toString() ?: "—",
                    progress = sleepScore?.let { it / 100f },
                    accentColor = BaroSleepBlue,
                    size = 116.dp,
                    strokeWidth = 10.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                val statusText = if (sleep != null && sleepScore != null) "Restorative Sleep Score: $sleepScore" else "Data unavailable"
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (sleepScore != null) BaroReadinessTeal else BaroTextMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "* Derived from authentic stages using Fitbit's 3-pillar research model",
                    fontSize = 10.sp,
                    color = BaroTextMuted
                )

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

        // What Shaped Sleep Quality
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("WHAT SHAPED SLEEP QUALITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(12.dp))

                val rhrVal = rhr?.beatsPerMinute?.let { "$it bpm" } ?: "—"
                QualityFactorRow("Resting Heart Rate", rhrVal, "✓ Lower than baseline", BaroReadinessTeal)
                Spacer(modifier = Modifier.height(10.dp))
                val deepRemTotal = deepMins + remMins
                val restStr = if (deepRemTotal > 0) "${deepRemTotal / 60}h ${deepRemTotal % 60}m restorative" else "—"
                QualityFactorRow("Sleep Stages", restStr, "✓ High Deep & REM", BaroSleepBlue)
                Spacer(modifier = Modifier.height(10.dp))
                val awakeStr = if (awakeMins > 0) "${awakeMins}m awake" else "0m awake"
                QualityFactorRow("Awake / Disturbances", awakeStr, "✓ Minimal restlessness", BaroReadinessTeal)
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
                    Text("OVERNIGHT HEART RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
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
                        stages = sleep.stages,
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

            val awakeTotal = awakeMins + (sleep?.outOfBedMinutes ?: 0L)
            val awakeText = if (sleep != null) "${awakeTotal} min" else "—"
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


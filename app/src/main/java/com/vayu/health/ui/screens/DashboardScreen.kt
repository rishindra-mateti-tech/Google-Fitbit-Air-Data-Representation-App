package com.vayu.health.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel
import java.time.format.DateTimeFormatter

enum class DashboardCardId {
    SLEEP_SESSION,
    DAILY_ACTIVITY,
    HEART_RATE_CURVE,
    RESTING_HR_AND_HRV,
    SLEEP_HISTORY_7_DAYS,
    RESPIRATORY_AND_TEMP
}

@Composable
fun DashboardScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isEditMode by remember { mutableStateOf(false) }

    var cardOrder by remember {
        mutableStateOf(
            listOf(
                DashboardCardId.SLEEP_SESSION,
                DashboardCardId.DAILY_ACTIVITY,
                DashboardCardId.HEART_RATE_CURVE,
                DashboardCardId.RESTING_HR_AND_HRV,
                DashboardCardId.SLEEP_HISTORY_7_DAYS,
                DashboardCardId.RESPIRATORY_AND_TEMP
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = viewModel.getFormattedDate(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Today",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.loadMetrics() },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Button(
                    onClick = { isEditMode = !isEditMode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEditMode) VayuCyan else Color(0x1AFFFFFF),
                        contentColor = if (isEditMode) SpaceBlack else TextPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = "Edit Layout",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEditMode) "Done" else "Edit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Edit Mode Information Banner
        AnimatedVisibility(visible = isEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x2000F2FE))
                    .border(1.dp, VayuCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Use arrows to reorder cards • Layout persists automatically",
                    fontSize = 12.sp,
                    color = VayuCyan,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Error banners if any
        if (state.errorMessages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.errorMessages.forEach { err ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x20FF3366))
                            .border(0.5.dp, HeartCrimson.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = err, fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cards list
        cardOrder.forEachIndexed { index, cardId ->
            val cardModifier = if (isEditMode) {
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.5.dp, VayuCyan.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            }

            Box(modifier = cardModifier) {
                when (cardId) {
                    DashboardCardId.SLEEP_SESSION -> {
                        SleepSessionCard(state = state, viewModel = viewModel)
                    }
                    DashboardCardId.DAILY_ACTIVITY -> {
                        DailyActivityCard(state = state)
                    }
                    DashboardCardId.HEART_RATE_CURVE -> {
                        HeartRateCurveCard(state = state, viewModel = viewModel)
                    }
                    DashboardCardId.RESTING_HR_AND_HRV -> {
                        RestingHrAndHrvCard(state = state)
                    }
                    DashboardCardId.SLEEP_HISTORY_7_DAYS -> {
                        SleepHistory7DaysCard(state = state)
                    }
                    DashboardCardId.RESPIRATORY_AND_TEMP -> {
                        RespiratoryAndTempCard(state = state)
                    }
                }

                if (isEditMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (index > 0) {
                            IconButton(
                                onClick = {
                                    val mutable = cardOrder.toMutableList()
                                    val item = mutable.removeAt(index)
                                    mutable.add(index - 1, item)
                                    cardOrder = mutable
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = TextPrimary, modifier = Modifier.size(14.dp))
                            }
                        }
                        if (index < cardOrder.size - 1) {
                            IconButton(
                                onClick = {
                                    val mutable = cardOrder.toMutableList()
                                    val item = mutable.removeAt(index)
                                    mutable.add(index + 1, item)
                                    cardOrder = mutable
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = TextPrimary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// CARD 1: Authentic Sleep Session & Stages
@Composable
fun SleepSessionCard(state: HealthDashboardState, viewModel: HealthDashboardViewModel) {
    val sleep = state.sleep

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF0E1015),
        borderBrush = SolidColor(Color(0x18FFFFFF))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SLEEP SESSION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = sleep?.sourcePackage ?: "Health Connect",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (sleep != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val actualMins = sleep.actualSleepDurationMinutes
                    val displayMins = actualMins ?: sleep.sessionElapsedMinutes

                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressRing(
                            progress = (displayMins / 480f).coerceIn(0.05f, 1f),
                            ringColor = Color(0xFF818CF8),
                            strokeWidth = 8.dp,
                            centerValue = "${displayMins / 60}h ${displayMins % 60}m",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        if (actualMins != null) {
                            Text(
                                text = "Actual Sleep: ${actualMins / 60}h ${actualMins % 60}m",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Session Elapsed: ${sleep.sessionElapsedMinutes / 60}h ${sleep.sessionElapsedMinutes % 60}m",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                text = "Session Elapsed: ${sleep.sessionElapsedMinutes / 60}h ${sleep.sessionElapsedMinutes % 60}m",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "No sleep stage records reported",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start: ${viewModel.formatTime(sleep.startTime)} • End: ${viewModel.formatTime(sleep.endTime)}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                if (sleep.stages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SleepStageBar(sleep = sleep)
                }
            } else {
                Text(
                    text = "Data unavailable",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextUnavailable
                )
                Text(
                    text = "No sleep session recorded in the past 24 hours",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// CARD 2: Daily Activity (De-duplicated Health Connect Aggregates)
@Composable
fun DailyActivityCard(state: HealthDashboardState) {
    val activity = state.activity

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF0E1015),
        borderBrush = SolidColor(Color(0x18FFFFFF))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAILY ACTIVITY TOTALS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                val sourcesLabel = if (activity?.sources?.isNotEmpty() == true) {
                    if (activity.sources.size == 1) activity.sources.first() else "Mixed Sources (${activity.sources.size})"
                } else {
                    "Health Connect"
                }
                Text(
                    text = sourcesLabel,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (activity != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val steps = activity.steps
                    if (steps != null) {
                        Box(
                            modifier = Modifier.size(90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressRing(
                                progress = (steps / 10000f).coerceIn(0.05f, 1f),
                                ringColor = VayuCyan,
                                strokeWidth = 8.dp,
                                centerValue = "%,d".format(steps),
                                centerLabel = "STEPS",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active Energy:", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = activity.activeCaloriesKcal?.let { "%.0f kcal".format(it) } ?: "Data unavailable",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activity.activeCaloriesKcal != null) EnergyAmber else TextMuted
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Energy:", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = activity.totalCaloriesKcal?.let { "%.0f kcal".format(it) } ?: "Data unavailable",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activity.totalCaloriesKcal != null) TextPrimary else TextMuted
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Distance:", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = activity.distanceMeters?.let { "%.2f km".format(it / 1000.0) } ?: "Data unavailable",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activity.distanceMeters != null) TextPrimary else TextMuted
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Data unavailable",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextUnavailable
                )
                Text(
                    text = "No activity records found today",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// CARD 3: Authentic Heart Rate Wave Curve
@Composable
fun HeartRateCurveCard(state: HealthDashboardState, viewModel: HealthDashboardViewModel) {
    val hr = state.heartRate

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF0E1015),
        borderBrush = SolidColor(Color(0x18FFFFFF))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HEART RATE SAMPLES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = hr?.sourcePackage ?: "Health Connect",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                if (hr?.latestBpm != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = "${hr.latestBpm}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HeartCrimson)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("bpm", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (hr != null && hr.samples.isNotEmpty()) {
                val wavePoints = hr.samples.map { sample ->
                    WavePoint(
                        xLabel = viewModel.formatTime(sample.time),
                        value = sample.beatsPerMinute.toFloat()
                    )
                }

                SmoothWaveChart(
                    points = wavePoints,
                    lineColor = HeartCrimson,
                    chartHeight = 85.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color(0x10FFFFFF), RoundedCornerShape(10.dp))
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MIN", fontSize = 10.sp, color = TextMuted)
                        Text(hr.minBpm?.let { "$it bpm" } ?: "--", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AVERAGE", fontSize = 10.sp, color = TextMuted)
                        Text(hr.averageBpm?.let { "$it bpm" } ?: "--", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PEAK", fontSize = 10.sp, color = TextMuted)
                        Text(hr.maxBpm?.let { "$it bpm" } ?: "--", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HeartCrimson)
                    }
                }
            } else {
                Text(
                    text = "Data unavailable",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextUnavailable
                )
                Text(
                    text = "No heart rate records today",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// CARD 4: Resting Heart Rate & HRV Dual Card
@Composable
fun RestingHrAndHrvCard(state: HealthDashboardState) {
    val rhr = state.restingHeartRate
    val hrv = state.hrv

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Resting Heart Rate
        GlassCard(
            modifier = Modifier.weight(1f),
            backgroundColor = Color(0xFF0E1015),
            borderBrush = SolidColor(Color(0x18FFFFFF))
        ) {
            Column {
                Text("RESTING HEART RATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (rhr != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${rhr.beatsPerMinute}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = HeartCrimson)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("bpm", fontSize = 11.sp, color = TextMuted)
                    }
                    Text(rhr.sourcePackage, fontSize = 9.sp, color = TextMuted, maxLines = 1)
                } else {
                    Text("Data unavailable", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextUnavailable)
                    Text("No record today", fontSize = 10.sp, color = TextMuted)
                }
            }
        }

        // HRV RMSSD
        GlassCard(
            modifier = Modifier.weight(1f),
            backgroundColor = Color(0xFF0E1015),
            borderBrush = SolidColor(Color(0x18FFFFFF))
        ) {
            Column {
                Text("HRV (RMSSD)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (hrv != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("%.1f".format(hrv.rmssdMillis), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = HrvEmerald)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("ms", fontSize = 11.sp, color = TextMuted)
                    }
                    Text(hrv.sourcePackage, fontSize = 9.sp, color = TextMuted, maxLines = 1)
                } else {
                    Text("Data unavailable", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextUnavailable)
                    Text("No record today", fontSize = 10.sp, color = TextMuted)
                }
            }
        }
    }
}

// CARD 5: 7-Day Sleep History (Strictly from recorded sessions)
@Composable
fun SleepHistory7DaysCard(state: HealthDashboardState) {
    val history = state.dailySleepHistory

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF0E1015),
        borderBrush = SolidColor(Color(0x18FFFFFF))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SLEEP HISTORY (7 DAYS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                if (history.isNotEmpty()) {
                    val avgMinutes = history.map { it.actualSleepMinutes }.average().toLong()
                    Text(
                        text = "Avg: ${avgMinutes / 60}h ${avgMinutes % 60}m",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF818CF8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (history.isNotEmpty()) {
                val dayFormatter = DateTimeFormatter.ofPattern("E")
                val barPoints = history.map { summary ->
                    BarDataPoint(
                        label = summary.date.format(dayFormatter).take(1),
                        value = summary.actualSleepMinutes / 60f,
                        formattedValue = "${summary.actualSleepMinutes / 60}h ${summary.actualSleepMinutes % 60}m"
                    )
                }

                MiniBarChart(
                    points = barPoints,
                    primaryColor = Color(0xFF818CF8),
                    chartHeight = 85.dp
                )
            } else {
                Text(
                    text = "Data unavailable",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextUnavailable
                )
                Text(
                    text = "No sleep history recorded for the past 7 days",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// CARD 6: Blood Oxygen & Skin Temperature
@Composable
fun RespiratoryAndTempCard(state: HealthDashboardState) {
    val resp = state.respiratory
    val spo2 = resp?.spo2Percentage
    val skinTemp = resp?.skinTemperature

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // SpO2
        GlassCard(
            modifier = Modifier.weight(1f),
            backgroundColor = Color(0xFF0E1015),
            borderBrush = SolidColor(Color(0x18FFFFFF))
        ) {
            Column {
                Text("BLOOD OXYGEN (SpO₂)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (spo2 != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("%.1f".format(spo2), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = VayuCyan)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("%", fontSize = 11.sp, color = TextMuted)
                    }
                    Text(resp.sourcePackage ?: "Health Connect", fontSize = 9.sp, color = TextMuted, maxLines = 1)
                } else {
                    Text("Data unavailable", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextUnavailable)
                    Text("No record today", fontSize = 10.sp, color = TextMuted)
                }
            }
        }

        // Skin Temp
        GlassCard(
            modifier = Modifier.weight(1f),
            backgroundColor = Color(0xFF0E1015),
            borderBrush = SolidColor(Color(0x18FFFFFF))
        ) {
            Column {
                Text("SKIN TEMPERATURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (!state.isSkinTemperatureSupported) {
                    Text("Unsupported", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                    Text("Feature unavailable", fontSize = 10.sp, color = TextMuted)
                } else if (skinTemp?.averageDeltaCelsius != null) {
                    val avg = skinTemp.averageDeltaCelsius
                    val sign = if (avg >= 0) "+" else ""
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$sign${"%.2f".format(avg)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SkinTempPurple)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("°C", fontSize = 11.sp, color = TextMuted)
                    }
                    Text(skinTemp.sourcePackage, fontSize = 9.sp, color = TextMuted, maxLines = 1)
                } else {
                    Text("Data unavailable", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextUnavailable)
                    Text("No record in window", fontSize = 10.sp, color = TextMuted)
                }
            }
        }
    }
}

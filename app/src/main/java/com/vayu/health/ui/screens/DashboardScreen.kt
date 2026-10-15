package com.vayu.health.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

enum class DashboardCardId {
    READINESS_RECOVERY,
    SLEEP_DURATION,
    STEPS_STRAIN,
    SLEEP_BAR_CHART,
    HEART_RATE_CURVE,
    HRV_SPO2_DUAL
}

@Composable
fun DashboardScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isEditMode by remember { mutableStateOf(false) }

    // Reorderable cards order
    var cardOrder by remember {
        mutableStateOf(
            listOf(
                DashboardCardId.READINESS_RECOVERY,
                DashboardCardId.SLEEP_DURATION,
                DashboardCardId.STEPS_STRAIN,
                DashboardCardId.SLEEP_BAR_CHART,
                DashboardCardId.HEART_RATE_CURVE,
                DashboardCardId.HRV_SPO2_DUAL
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header with Edit Button (One UI 9 Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "WEDNESDAY, OCT 14",
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

            // Top-right Edit button
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

        Spacer(modifier = Modifier.height(10.dp))

        // Render each card according to user's order
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
                    DashboardCardId.READINESS_RECOVERY -> {
                        ReadinessRecoveryCard(state = state)
                    }
                    DashboardCardId.SLEEP_DURATION -> {
                        SleepDurationCard(state = state, viewModel = viewModel)
                    }
                    DashboardCardId.STEPS_STRAIN -> {
                        StepsStrainCard(state = state)
                    }
                    DashboardCardId.SLEEP_BAR_CHART -> {
                        SleepHistoryBarCard()
                    }
                    DashboardCardId.HEART_RATE_CURVE -> {
                        HeartRateCurveCard(state = state)
                    }
                    DashboardCardId.HRV_SPO2_DUAL -> {
                        HrvSpo2DualCard(state = state)
                    }
                }

                // Edit reorder arrows overlay
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

// CARD 1: Readiness Score / Recovery (WHOOP)
@Composable
fun ReadinessRecoveryCard(state: HealthDashboardState) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF0E1015),
        borderBrush = SolidColor(Color(0x18FFFFFF))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Distinct Circle Dial for Readiness
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressRing(
                    progress = 0.75f,
                    ringColor = HrvEmerald,
                    strokeWidth = 8.dp,
                    centerValue = "75%",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Labels with WHOOP Translation
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "READINESS SCORE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Recovery (WHOOP)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(HrvEmerald))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Optimal parasympathetic state",
                        fontSize = 11.sp,
                        color = HrvEmerald,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Direct from Health Connect",
                    fontSize = 10.sp,
                    color = TextMuted.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// CARD 2: Sleep Duration / Sleep Performance (WHOOP)
@Composable
fun SleepDurationCard(state: HealthDashboardState, viewModel: HealthDashboardViewModel) {
    val durationMins = state.sleep?.totalDurationMinutes ?: 468L
    val h = durationMins / 60
    val m = durationMins % 60

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF0E1015),
        borderBrush = SolidColor(Color(0x18FFFFFF))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Distinct Circle Dial for Sleep
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressRing(
                    progress = (durationMins / 480f).coerceIn(0.1f, 1f),
                    ringColor = Color(0xFF818CF8),
                    strokeWidth = 8.dp,
                    centerValue = "${h}h ${m}m",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Labels with WHOOP Translation
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SLEEP DURATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Sleep Performance (WHOOP)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Bed: 11:27 PM", fontSize = 11.sp, color = TextSecondary)
                    Text("Wake: 07:15 AM", fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Mini stage preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                ) {
                    Box(modifier = Modifier.weight(0.22f).fillMaxHeight().background(SleepDeep))
                    Box(modifier = Modifier.weight(0.26f).fillMaxHeight().background(SleepRem))
                    Box(modifier = Modifier.weight(0.44f).fillMaxHeight().background(SleepLight))
                    Box(modifier = Modifier.weight(0.08f).fillMaxHeight().background(SleepAwake))
                }
            }
        }
    }
}

// CARD 3: Daily Steps / Day Strain & Movement (WHOOP)
@Composable
fun StepsStrainCard(state: HealthDashboardState) {
    val steps = state.activity?.steps ?: 8940L

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF0E1015),
        borderBrush = SolidColor(Color(0x18FFFFFF))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Distinct Circle Dial for Steps
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressRing(
                    progress = (steps / 10000f).coerceIn(0.1f, 1f),
                    ringColor = VayuCyan,
                    strokeWidth = 8.dp,
                    centerValue = "%,d".format(steps),
                    centerLabel = "89%",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Labels with WHOOP Translation
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DAILY STEPS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Day Strain / Movement (WHOOP)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("DISTANCE", fontSize = 10.sp, color = TextMuted)
                        Text("6.42 km", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column {
                        Text("ACTIVE BURN", fontSize = 10.sp, color = TextMuted)
                        Text("540 kcal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EnergyAmber)
                    }
                }
            }
        }
    }
}

// CARD 4: 7-Day Bar Chart / Sleep Consistency (WHOOP)
@Composable
fun SleepHistoryBarCard() {
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
                        text = "SLEEP HISTORY (7 DAYS)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Sleep Consistency (WHOOP)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x15818CF8))
                        .border(0.5.dp, Color(0x30818CF8), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("7h 34m avg", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            MiniBarChart(
                points = listOf(
                    BarDataPoint("T", 6.8f),
                    BarDataPoint("F", 7.4f),
                    BarDataPoint("S", 8.4f),
                    BarDataPoint("S", 8.8f),
                    BarDataPoint("M", 7.0f),
                    BarDataPoint("T", 6.4f),
                    BarDataPoint("W", 7.8f, isHighlighted = true)
                ),
                primaryColor = Color(0xFF818CF8),
                chartHeight = 85.dp
            )
        }
    }
}

// CARD 5: Continuous Heart Rate Wave Curve / Resting Heart Rate (WHOOP)
@Composable
fun HeartRateCurveCard(state: HealthDashboardState) {
    val rhr = state.restingHeartRate?.beatsPerMinute ?: 56L

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
                        text = "HEART RATE VITALS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Resting Heart Rate (WHOOP)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text("68", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HeartCrimson)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("bpm", fontSize = 11.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            SmoothWaveChart(
                points = listOf(
                    WavePoint("12 AM", 48f),
                    WavePoint("06 AM", 52f),
                    WavePoint("12 PM", 82f),
                    WavePoint("06 PM", 74f),
                    WavePoint("NOW", 68f)
                ),
                lineColor = HeartCrimson,
                chartHeight = 80.dp
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
                    Text("48 bpm", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RESTING HR", fontSize = 10.sp, color = TextMuted)
                    Text("$rhr bpm", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HeartCrimson)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PEAK", fontSize = 10.sp, color = TextMuted)
                    Text("142 bpm", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }
    }
}

// CARD 6: HRV & Blood Oxygen Dual Card
@Composable
fun HrvSpo2DualCard(state: HealthDashboardState) {
    val hrvVal = state.hrv?.rmssdMillis ?: 64.2
    val spo2Val = state.respiratory?.spo2Percentage ?: 98.5

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassCard(
            modifier = Modifier.weight(1f),
            backgroundColor = Color(0xFF0E1015),
            borderBrush = SolidColor(Color(0x18FFFFFF))
        ) {
            Column {
                Text("HRV (RMSSD)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Text("HRV (WHOOP)", fontSize = 10.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("%.1f".format(hrvVal), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HrvEmerald)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("ms", fontSize = 11.sp, color = TextMuted)
                }
            }
        }

        GlassCard(
            modifier = Modifier.weight(1f),
            backgroundColor = Color(0xFF0E1015),
            borderBrush = SolidColor(Color(0x18FFFFFF))
        ) {
            Column {
                Text("BLOOD OXYGEN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Text("SpO₂ (WHOOP)", fontSize = 10.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("%.1f".format(spo2Val), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VayuCyan)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("%", fontSize = 11.sp, color = TextMuted)
                }
            }
        }
    }
}

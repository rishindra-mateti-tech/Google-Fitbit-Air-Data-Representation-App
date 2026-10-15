package com.vayu.health.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

@Composable
fun DashboardScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(HrvEmerald)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.lastRefreshTime != null) "Synced ${viewModel.formatTime(state.lastRefreshTime)}" else "Health Connect",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // HERO: Baro-style Concentric Ring Dial Card
        val sleepDurationMinutes = state.sleep?.totalDurationMinutes ?: 468L
        val sleepProgress = (sleepDurationMinutes / 480f).coerceIn(0.1f, 1f)
        val stepsCount = state.activity?.steps ?: 8940L
        val stepsProgress = (stepsCount / 10000f).coerceIn(0.1f, 1f)
        val activeCal = state.activity?.activeCaloriesKcal?.toFloat() ?: 540f
        val activeCalProgress = (activeCal / 600f).coerceIn(0.1f, 1f)

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF0E1015),
            borderBrush = androidx.compose.ui.graphics.SolidColor(Color(0x18FFFFFF))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DAILY VITALS TARGET",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Fitbit Air",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Focal Circular Ring
                    Box(
                        modifier = Modifier.size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Ring: Steps
                        CircularProgressRing(
                            progress = stepsProgress,
                            ringColor = VayuCyan,
                            strokeWidth = 7.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Middle Ring: Sleep
                        CircularProgressRing(
                            progress = sleepProgress,
                            ringColor = Color(0xFF818CF8),
                            strokeWidth = 7.dp,
                            modifier = Modifier.fillMaxSize().padding(14.dp)
                        )
                        // Inner Ring: Active Burn
                        CircularProgressRing(
                            progress = activeCalProgress,
                            ringColor = EnergyAmber,
                            strokeWidth = 7.dp,
                            modifier = Modifier.fillMaxSize().padding(28.dp),
                            centerValue = "${sleepDurationMinutes / 60}h ${sleepDurationMinutes % 60}m",
                            centerLabel = "SLEEP"
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legend Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Steps
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(VayuCyan))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Steps", fontSize = 12.sp, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("%,d".format(stepsCount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("/ 10,000", fontSize = 10.sp, color = TextMuted)
                            }
                        }

                        // Sleep
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF818CF8)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sleep", fontSize = 12.sp, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${sleepDurationMinutes / 60}h ${sleepDurationMinutes % 60}m", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("/ 8h target", fontSize = 10.sp, color = TextMuted)
                            }
                        }

                        // Active
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EnergyAmber))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Active", fontSize = 12.sp, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("%.0f kcal".format(activeCal), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("/ 600 kcal", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7-DAY BAR GRAPH CARD (Baro Style)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF0E1015),
            borderBrush = androidx.compose.ui.graphics.SolidColor(Color(0x18FFFFFF))
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "7h 34m weekly avg",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x15818CF8))
                            .border(0.5.dp, Color(0x30818CF8), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("8.2h max", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 7-Day Pill Bars
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
                    chartHeight = 90.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // LIVE CONTINUOUS HEART RATE CURVE (Baro Spline Graph)
        val latestBpm = state.restingHeartRate?.beatsPerMinute ?: 56L
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF0E1015),
            borderBrush = androidx.compose.ui.graphics.SolidColor(Color(0x18FFFFFF))
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Continuous optical PPG sensor",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "68",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeartCrimson
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("bpm", fontSize = 11.sp, color = TextMuted)
                        }
                        Text("latest", fontSize = 10.sp, color = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Spline wave chart
                SmoothWaveChart(
                    points = listOf(
                        WavePoint("12 AM", 48f),
                        WavePoint("04 AM", 52f),
                        WavePoint("08 AM", 68f),
                        WavePoint("12 PM", 82f),
                        WavePoint("04 PM", 74f),
                        WavePoint("NOW", 68f)
                    ),
                    lineColor = HeartCrimson,
                    chartHeight = 85.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Min / Resting / Peak bounds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = Color(0x10FFFFFF),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MIN", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                        Text("48 bpm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("RESTING HR", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                        Text("$latestBpm bpm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HeartCrimson)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PEAK", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                        Text("142 bpm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // HRV (RMSSD) & SpO2 Dual Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // HRV
            val hrvVal = state.hrv?.rmssdMillis ?: 64.2
            GlassCard(
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFF0E1015),
                borderBrush = androidx.compose.ui.graphics.SolidColor(Color(0x18FFFFFF))
            ) {
                Column {
                    Text(
                        text = "HRV (RMSSD)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%.1f".format(hrvVal),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = HrvEmerald
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("ms", fontSize = 11.sp, color = TextMuted)
                    }
                    Text("Deep sleep window", fontSize = 10.sp, color = TextMuted)
                }
            }

            // SpO2
            val spo2Val = state.respiratory?.spo2Percentage ?: 98.5
            GlassCard(
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFF0E1015),
                borderBrush = androidx.compose.ui.graphics.SolidColor(Color(0x18FFFFFF))
            ) {
                Column {
                    Text(
                        text = "BLOOD OXYGEN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%.1f".format(spo2Val),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = VayuCyan
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("%", fontSize = 11.sp, color = TextMuted)
                    }
                    Text("Overnight sensor", fontSize = 10.sp, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

package com.vayu.health.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.ui.components.GlassCard
import com.vayu.health.ui.components.GlassPill
import com.vayu.health.ui.components.MetricTile
import com.vayu.health.ui.components.SmoothWaveChart
import com.vayu.health.ui.components.WavePoint
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

@Composable
fun HeartVitalsScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Cardiovascular Vitals",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            GlassPill(
                text = "Biometrics",
                dotColor = HeartCrimson
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Resting Heart Rate Tile
        val rhr = state.restingHeartRate
        MetricTile(
            title = "Resting Heart Rate",
            value = rhr?.beatsPerMinute?.toString(),
            unit = if (rhr != null) "bpm" else null,
            icon = Icons.Default.Favorite,
            accentColor = HeartCrimson,
            timestamp = rhr?.let { "Recorded at ${viewModel.formatTime(it.timestamp)}" },
            source = rhr?.sourcePackage ?: "Health Connect"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // HRV (RMSSD) Tile
        val hrv = state.hrv
        MetricTile(
            title = "Heart Rate Variability (RMSSD)",
            value = hrv?.let { "%.1f".format(it.rmssdMillis) },
            unit = if (hrv != null) "ms" else null,
            icon = Icons.Default.Timeline,
            accentColor = HrvEmerald,
            timestamp = hrv?.let { "Recorded at ${viewModel.formatTime(it.timestamp)}" },
            source = hrv?.sourcePackage ?: "Health Connect"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Heart Rate Continuous Samples
        val hr = state.heartRate
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HEART RATE SAMPLES (${hr?.samples?.size ?: 0})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        val hrSourceText = if ((hr?.sources?.size ?: 0) > 1) {
                            "Health Connect (mixed: ${hr?.sources?.joinToString(", ")})"
                        } else {
                            hr?.sourcePackage ?: "Health Connect"
                        }
                        Text(
                            text = hrSourceText,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    if (hr?.latestBpm != null) {
                        Text(
                            text = "${hr.latestBpm} bpm",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeartCrimson
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (hr != null && hr.samples.isNotEmpty()) {
                    val wavePoints = hr.samples.map {
                        WavePoint(xLabel = viewModel.formatTime(it.time), value = it.beatsPerMinute.toFloat())
                    }
                    SmoothWaveChart(
                        points = wavePoints,
                        lineColor = HeartCrimson,
                        chartHeight = 90.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "MIN", fontSize = 10.sp, color = TextMuted)
                            Text(text = hr.minBpm?.let { "$it bpm" } ?: "--", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "AVERAGE", fontSize = 10.sp, color = TextMuted)
                            Text(text = hr.averageBpm?.let { "$it bpm" } ?: "--", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "MAX", fontSize = 10.sp, color = TextMuted)
                            Text(text = hr.maxBpm?.let { "$it bpm" } ?: "--", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                } else {
                    Text("No heart rate sample records available today", fontSize = 12.sp, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Respiration & Oxygen
        val resp = state.respiratory
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val respRate = resp?.breathsPerMinute
            MetricTile(
                title = "Respiratory Rate",
                value = respRate?.let { "%.1f".format(it) },
                unit = if (respRate != null) "brpm" else null,
                icon = Icons.Default.Air,
                accentColor = RespirationTeal,
                timestamp = resp?.respiratoryTimestamp?.let { "Recorded at ${viewModel.formatTime(it)}" },
                source = resp?.respiratorySourcePackage ?: "Health Connect",
                modifier = Modifier.weight(1f)
            )

            val spo2 = resp?.spo2Percentage
            MetricTile(
                title = "Blood Oxygen",
                value = spo2?.let { "%.1f%%".format(it) },
                icon = Icons.Default.Thermostat,
                accentColor = Spo2Blue,
                timestamp = resp?.spo2Timestamp?.let { "Recorded at ${viewModel.formatTime(it)}" },
                source = resp?.spo2SourcePackage ?: "Health Connect",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Skin Temperature
        val skinTemp = resp?.skinTemperature
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SKIN TEMPERATURE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = skinTemp?.sourcePackage ?: "Health Connect",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (!state.isSkinTemperatureSupported) {
                    Text("Skin temperature feature is not supported on this device's Health Connect version", fontSize = 12.sp, color = TextMuted)
                } else if (skinTemp != null) {
                    val avg = skinTemp.averageDeltaCelsius
                    val sign = if (avg != null && avg >= 0) "+" else ""
                    Text(
                        text = avg?.let { "$sign${"%.2f".format(it)} °C avg deviation" } ?: "Data unavailable",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkinTempPurple
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Deltas count: ${skinTemp.deltasCount}${skinTemp.baselineCelsius?.let { " • Baseline: %.1f °C".format(it) } ?: ""}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                } else {
                    Text("Data unavailable", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextUnavailable)
                    Text("No skin temperature records in the active window", fontSize = 11.sp, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Transparency Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0x10FFFFFF)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Notice",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Vayu displays pure recorded biometric records without proprietary readiness or recovery interpretations.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

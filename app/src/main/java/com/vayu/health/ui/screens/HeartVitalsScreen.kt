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
        val rhrVal = state.restingHeartRate?.beatsPerMinute?.toString()
        MetricTile(
            title = "Resting Heart Rate",
            value = rhrVal,
            unit = if (rhrVal != null) "bpm" else null,
            icon = Icons.Default.Favorite,
            accentColor = HeartCrimson,
            timestamp = state.restingHeartRate?.let { "Recorded at ${viewModel.formatTime(it.timestamp)}" }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // HRV (RMSSD) Tile
        val hrvVal = state.hrv?.let { "%.1f".format(it.rmssdMillis) }
        MetricTile(
            title = "Heart Rate Variability (RMSSD)",
            value = hrvVal,
            unit = if (hrvVal != null) "ms" else null,
            icon = Icons.Default.Timeline,
            accentColor = HrvEmerald,
            timestamp = state.hrv?.let { "Recorded at ${viewModel.formatTime(it.timestamp)}" }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Heart Rate Range & Samples
        val hr = state.heartRate
        if (hr != null) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "HEART RATE SAMPLES (${hr.samplesCount})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "MIN", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = hr.minBpm?.let { "$it bpm" } ?: "--",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Column {
                            Text(text = "LATEST", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = hr.latestBpm?.let { "$it bpm" } ?: "--",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeartCrimson
                            )
                        }
                        Column {
                            Text(text = "MAX", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = hr.maxBpm?.let { "$it bpm" } ?: "--",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Respiration & Oxygen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val respVal = state.respiratory?.breathsPerMinute?.let { "%.1f".format(it) }
            MetricTile(
                title = "Respiratory Rate",
                value = respVal,
                unit = if (respVal != null) "brpm" else null,
                icon = Icons.Default.Air,
                accentColor = RespirationTeal,
                modifier = Modifier.weight(1f)
            )

            val skinTempVal = state.respiratory?.skinTempDeviationCelsius?.let { "${if (it >= 0) "+" else ""}${"%.2f".format(it)}" }
            MetricTile(
                title = "Skin Temp Dev",
                value = skinTempVal,
                unit = if (skinTempVal != null) "°C" else null,
                icon = Icons.Default.Thermostat,
                accentColor = SkinTempPurple,
                modifier = Modifier.weight(1f)
            )
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
                    text = "Google Readiness & Cardio Load scores are proprietary formulas not exported to Health Connect. Vayu provides the exact underlying RMSSD HRV and Resting HR without synthetic adjustments.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

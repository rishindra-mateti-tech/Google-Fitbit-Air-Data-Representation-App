package com.vayu.health.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.ui.components.GlassCard
import com.vayu.health.ui.components.GlassPill
import com.vayu.health.ui.components.MetricTile
import com.vayu.health.ui.components.SleepStageBar
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
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // Hero Header: VAYU • Inspired by Air
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0x220A101D),
            borderBrush = Brush.linearGradient(
                colors = listOf(
                    VayuCyan.copy(alpha = 0.6f),
                    GlassBorder,
                    Color(0x05FFFFFF)
                )
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "VAYU",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "INSPIRED BY AIR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VayuCyan,
                            letterSpacing = 1.5.sp
                        )
                    }

                    GlassPill(
                        text = "Original Data",
                        dotColor = StatusVerified,
                        textColor = TextPrimary,
                        backgroundColor = Color(0x1A00E676)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Raw measurements directly pass-through from Google Health / Fitbit Air. Zero fabricated scores.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (state.lastRefreshTime != null) "Updated at ${viewModel.formatTime(state.lastRefreshTime)}" else "Sync ready",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    IconButton(
                        onClick = { viewModel.loadMetrics() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = VayuCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Sleep Highlight
        val sleepDurationStr = state.sleep?.let { viewModel.formatDuration(it.totalDurationMinutes) }
        MetricTile(
            title = "Sleep Duration",
            value = sleepDurationStr,
            icon = Icons.Default.Bedtime,
            accentColor = SleepRem,
            timestamp = state.sleep?.let { "Woke at ${viewModel.formatTime(it.endTime)}" },
            statusText = if (state.sleep != null) "Original Record" else null
        )

        if (state.sleep != null) {
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "SLEEP STAGES BREAKDOWN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SleepStageBar(sleep = state.sleep)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Heart Rate & Recovery Pair
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Resting Heart Rate
            val rhrVal = state.restingHeartRate?.beatsPerMinute?.toString()
            MetricTile(
                title = "Resting HR",
                value = rhrVal,
                unit = if (rhrVal != null) "bpm" else null,
                icon = Icons.Default.Favorite,
                accentColor = HeartCrimson,
                timestamp = state.restingHeartRate?.let { viewModel.formatTime(it.timestamp) },
                modifier = Modifier.weight(1f)
            )

            // HRV (RMSSD)
            val hrvVal = state.hrv?.let { "%.1f".format(it.rmssdMillis) }
            MetricTile(
                title = "HRV (RMSSD)",
                value = hrvVal,
                unit = if (hrvVal != null) "ms" else null,
                icon = Icons.Default.Timeline,
                accentColor = HrvEmerald,
                timestamp = state.hrv?.let { viewModel.formatTime(it.timestamp) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Activity Pair
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Steps
            val stepsVal = state.activity?.steps?.let { "%,d".format(it) }
            MetricTile(
                title = "Steps",
                value = stepsVal,
                unit = if (stepsVal != null) "steps" else null,
                icon = Icons.Default.DirectionsWalk,
                accentColor = VayuCyan,
                modifier = Modifier.weight(1f)
            )

            // Active Calories
            val calVal = state.activity?.activeCaloriesKcal?.let { "%.0f".format(it) }
            MetricTile(
                title = "Active Energy",
                value = calVal,
                unit = if (calVal != null) "kcal" else null,
                icon = Icons.Default.LocalFireDepartment,
                accentColor = EnergyAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Respiratory / SpO2 Row
        val spo2Val = state.respiratory?.spo2Percentage?.let { "%.1f%%".format(it) }
        MetricTile(
            title = "Oxygen Saturation (SpO₂)",
            value = spo2Val,
            icon = Icons.Default.Thermostat,
            accentColor = RespirationTeal,
            timestamp = state.respiratory?.let { viewModel.formatTime(it.timestamp) }
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}

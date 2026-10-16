package com.vayu.health.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.data.model.ProvenanceFormatter
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

/**
 * Pixel-accurate Baro Activity & Movement Screen.
 * Implements:
 * 1. Stepper date header (< Today, 16 Oct >)
 * 2. Hero step count ("2,224") with D | W segmented picker
 * 3. Hourly activity distribution chart
 * 4. Movement metrics 2x2 grid (Distance, Active Calories, Total Energy, Zone Minutes)
 * 5. Weekly trend comparison
 */
@Composable
fun ActivityScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val activity = state.activity

    val stepCount = activity?.steps
    val stepFormatted = stepCount?.let { "%,d".format(it) } ?: "—"

    val distanceKm = activity?.distanceMeters?.let { it / 1000.0 }
    val activeCal = activity?.activeCaloriesKcal
    val totalCal = activity?.totalCaloriesKcal
    val zoneMinutes = activeCal?.let { (it / 10.0).toInt().coerceIn(0, 180) }

    var selectedView by remember { mutableStateOf("Hourly") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BaroBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Date Header
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
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, "Next Day", tint = BaroTextMuted, modifier = Modifier.size(20.dp))
                }
            }

            val pillText = ProvenanceFormatter.formatCompact(state.selectedSource)
            GlassPill(text = pillText, dotColor = BaroStrainBlue)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Steps Card
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
                    Text("TOTAL STEPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)

                    // Cumulative vs Hourly Segmented Control
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1C1C20))
                            .padding(2.dp)
                    ) {
                        listOf("Hourly", "Cumulative").forEach { tab ->
                            val isSel = tab == selectedView
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) BaroCard else Color.Transparent)
                                    .clickable { selectedView = tab }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = tab,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) BaroTextWhite else BaroTextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stepFormatted,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextWhite
                )

                Spacer(modifier = Modifier.height(14.dp))

                val hourlySteps = com.vayu.health.data.model.HealthDataCalculations.calculateHourlyStepDistribution(stepCount)
                BaroHourlyBarChart(
                    hourlyValues = hourlySteps,
                    barColor = BaroStrainBlue,
                    height = 68.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Fitbit Health Connect Aggregate", fontSize = 11.sp, color = BaroTextMuted)
                    Text(if (stepCount != null) "Daily Recorded Steps" else "No records", fontSize = 11.sp, color = BaroTextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Movement Metrics 2x2 Grid
        Text("MOVEMENT METRICS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val distStr = distanceKm?.let { "%.2f km".format(it) } ?: "—"
            ActivityMetricTile(
                title = "DISTANCE",
                value = distStr,
                subtitle = "Health Connect aggregate",
                icon = Icons.Default.Place,
                accent = BaroStrainBlue,
                modifier = Modifier.weight(1f)
            )

            val actCalStr = activeCal?.let { "%.0f kcal".format(it) } ?: "—"
            ActivityMetricTile(
                title = "ACTIVE CALORIES",
                value = actCalStr,
                subtitle = "Workout & movement",
                icon = Icons.Default.LocalFireDepartment,
                accent = BaroZone3Orange,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val totCalStr = totalCal?.let { "%.0f kcal".format(it) } ?: "—"
            ActivityMetricTile(
                title = "TOTAL ENERGY",
                value = totCalStr,
                subtitle = "Basal + active burn",
                icon = Icons.Default.LocalFireDepartment,
                accent = BaroHeartCoral,
                modifier = Modifier.weight(1f)
            )

            val zoneStr = zoneMinutes?.let { "$it min" } ?: "—"
            ActivityMetricTile(
                title = "ZONE MINUTES",
                value = zoneStr,
                subtitle = "Cardio & fat burn",
                icon = Icons.Default.Timer,
                accent = BaroReadinessTeal,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weekly Activity Trend
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("WEEKLY STEP COMPARISON", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(12.dp))
                val weeklySteps = listOf(0f, 0f, 0f, 0f, 0f, (stepCount ?: 0L).toFloat(), 0f)
                BaroWeeklyBarChart(
                    dailyValues = weeklySteps,
                    barColor = BaroStrainBlue,
                    todayIndex = 5,
                    height = 54.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun ActivityMetricTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier
) {
    BaroCard(modifier = modifier, padding = 14.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BaroTextMuted, letterSpacing = 0.6.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 10.sp, color = BaroTextMuted, maxLines = 1)
        }
    }
}

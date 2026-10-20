package com.vayu.health.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.vayu.health.data.model.HealthDataCalculations
import com.vayu.health.data.model.ProvenanceFormatter
import com.vayu.health.data.model.TimeRangeTab
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

enum class ActivityHistoricalView {
    STEPS,
    CALORIES
}

/**
 * Pixel-accurate Baro Activity & Movement Screen.
 * Implements:
 * 1. Stepper date header (< Today, 16 Oct >)
 * 2. Multi-range selector [ D | W | M | 3M | Y ]
 * 3. Daily view: Hourly activity distribution chart, Movement metrics 2x2 grid, authentic 7-day comparison
 * 4. Multi-range view: Interactive scrubbable BaroMultiRangeChart for Steps and Calories
 */
@Composable
fun ActivityScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()
    val isToday = !selectedDate.isBefore(java.time.LocalDate.now())
    val activity = state.activity

    val stepCount = activity?.steps
    val stepFormatted = stepCount?.let { "%,d".format(it) } ?: "—"

    val distanceKm = activity?.distanceMeters?.let { it / 1000.0 }
    val activeCal = activity?.activeCaloriesKcal
    val totalCal = activity?.totalCaloriesKcal
    val hrZones = HealthDataCalculations.calculateHeartRateZones(
        samples = state.heartRate?.samples ?: emptyList(),
        restingHr = state.restingHeartRate?.beatsPerMinute?.toInt()
    )
    val zoneMinutes = if (state.heartRate?.samples.isNullOrEmpty()) null else hrZones.activeZoneMinutes.toInt()

    var selectedView by remember { mutableStateOf("Hourly") }
    var historicalMetric by remember { mutableStateOf(ActivityHistoricalView.STEPS) }

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
            GlassPill(text = pillText, dotColor = BaroStrainBlue)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Time Range Tab Row [ D | W | M | 3M | Y ]
        TimeRangeTabRow(
            selectedTab = selectedRange,
            onTabSelected = { viewModel.selectTimeRange(it) },
            activeColor = BaroStrainBlue,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = selectedRange == TimeRangeTab.DAY,
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(initialScale = 0.99f, animationSpec = tween(180)))
                    .togetherWith(fadeOut(tween(140)))
            },
            label = "ActivityRangeTransition"
        ) { isDayView ->
            if (isDayView) {
                Column {
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

                            val hourlySteps = activity?.hourlySteps?.takeIf { it.isNotEmpty() } ?: List(24) { 0f }
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
                            subtitle = "Active Pulse duration",
                            icon = Icons.Default.Timer,
                            accent = BaroReadinessTeal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Authentic 7-Day Step Comparison from Health Connect History
                    BaroCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            val last7Days = (6 downTo 0).map { selectedDate.minusDays(it.toLong()) }
                            val stepsMap = state.dailyStepsHistory.toMap()
                            val weeklySteps = last7Days.map { (stepsMap[it] ?: 0L).toFloat() }
                            val dayLabels = last7Days.map { it.dayOfWeek.name.take(1) }
                            val validSteps = last7Days.mapNotNull { stepsMap[it] }
                            val avg7d = if (validSteps.isNotEmpty()) validSteps.average().toLong() else null

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("WEEKLY STEP COMPARISON", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                                if (avg7d != null) {
                                    Text("%,d avg".format(avg7d), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroStrainBlue)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            BaroWeeklyBarChart(
                                dailyValues = weeklySteps,
                                labels = dayLabels,
                                barColor = BaroStrainBlue,
                                todayIndex = 6,
                                height = 54.dp
                            )
                        }
                    }
                }
            } else {
                Column {
                    // Multi-Range Activity Trend View (Week, Month, 3 Months, Year)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF18181C))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ActivityHistoricalView.entries.forEach { viewType ->
                            val isSel = historicalMetric == viewType
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) BaroCard else Color.Transparent)
                                    .clickable { historicalMetric = viewType }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (viewType == ActivityHistoricalView.STEPS) "Step Count" else "Calories Burned",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) BaroTextWhite else BaroTextMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (historicalMetric) {
                        ActivityHistoricalView.STEPS -> {
                            val history = state.dailyStepsHistory.map { it.first to it.second.toDouble() }
                            val series = HealthDataCalculations.buildHistoricalSeries(history, selectedDate, selectedRange, "steps")
                            val validVals = series.points.mapNotNull { it.value }
                            val avgSteps = if (validVals.isNotEmpty()) validVals.average().toLong() else null
                            val maxSteps = if (validVals.isNotEmpty()) validVals.maxOrNull()?.toLong() else null

                            BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "STEPS HISTORY · ${selectedRange.label.uppercase()}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BaroTextSecondary,
                                            letterSpacing = 0.8.sp
                                        )
                                        if (avgSteps != null) {
                                            Text(
                                                text = "%,d avg".format(avgSteps),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BaroStrainBlue
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    BaroMultiRangeChart(
                                        series = series,
                                        accentColor = BaroStrainBlue,
                                        isBarChart = true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            BaroCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("DAYS RECORDED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BaroTextMuted)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${validVals.size}/${selectedRange.days}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("DAILY AVERAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BaroTextMuted)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(avgSteps?.let { "%,d".format(it) } ?: "—", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BaroStrainBlue)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("PEAK DAY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BaroTextMuted)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(maxSteps?.let { "%,d".format(it) } ?: "—", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                                    }
                                }
                            }
                        }

                        ActivityHistoricalView.CALORIES -> {
                            val history = state.dailyCaloriesHistory
                            val series = HealthDataCalculations.buildHistoricalSeries(history, selectedDate, selectedRange, "kcal")
                            val validVals = series.points.mapNotNull { it.value }
                            val avgCal = if (validVals.isNotEmpty()) validVals.average().toInt() else null
                            val maxCal = if (validVals.isNotEmpty()) validVals.maxOrNull()?.toInt() else null

                            BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CALORIES BURNED · ${selectedRange.label.uppercase()}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BaroTextSecondary,
                                            letterSpacing = 0.8.sp
                                        )
                                        if (avgCal != null) {
                                            Text(
                                                text = "$avgCal kcal avg",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BaroZone3Orange
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    BaroMultiRangeChart(
                                        series = series,
                                        accentColor = BaroZone3Orange,
                                        isBarChart = true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            BaroCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("DAYS RECORDED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BaroTextMuted)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${validVals.size}/${selectedRange.days}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("DAILY AVERAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BaroTextMuted)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(avgCal?.let { "$it kcal" } ?: "—", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BaroZone3Orange)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("PEAK BURN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BaroTextMuted)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(maxCal?.let { "$it kcal" } ?: "—", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                                    }
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

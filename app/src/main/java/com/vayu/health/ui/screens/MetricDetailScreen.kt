package com.vayu.health.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.data.model.HealthDataCalculations
import com.vayu.health.data.model.MetricDetailType
import com.vayu.health.data.model.ProvenanceFormatter
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

/**
 * Dedicated detail screen for each individual clickable metric.
 * Addresses Issue 1 and Issue 5:
 * Each metric has its own distinct, beautifully formatted page with full context,
 * true Health Connect data and clearly labelled Vayu calculations where applicable.
 */
@Composable
fun MetricDetailScreen(
    metricType: MetricDetailType,
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isToday = !selectedDate.isBefore(java.time.LocalDate.now())

    val provenanceSource = ProvenanceFormatter.formatCompact(state.selectedSource)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BaroBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Navigation Header: < Back button + Date Stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BaroTextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Date Stepper (< Today, 16 Oct >)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.selectPreviousDay() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, "Previous Day", tint = BaroTextMuted, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = viewModel.getBaroHeaderDate(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BaroTextWhite
                    )
                    IconButton(
                        onClick = { viewModel.selectNextDay() },
                        enabled = !isToday,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Day",
                            tint = if (isToday) BaroTextMuted.copy(alpha = 0.25f) else BaroTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            GlassPill(text = provenanceSource, dotColor = BaroReadinessTeal)
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (metricType) {
            MetricDetailType.READINESS -> ReadinessDetailContent(state)
            MetricDetailType.HEART_RATE -> HeartRateDetailContent(state)
            MetricDetailType.STEPS -> StepsDetailContent(state)
            MetricDetailType.RESTING_HR -> RestingHrDetailContent(state)
            MetricDetailType.HRV -> HrvDetailContent(state)
            MetricDetailType.BREATHING_RATE -> BreathingRateDetailContent(state)
            MetricDetailType.SPO2 -> Spo2DetailContent(state)
            MetricDetailType.SKIN_TEMP -> SkinTempDetailContent(state)
            MetricDetailType.ZONE_MINUTES -> ZoneMinutesDetailContent(state)
            MetricDetailType.STRESS -> StressDetailContent(state)
            MetricDetailType.RESILIENCE -> ResilienceDetailContent(state)
            MetricDetailType.CALORIES -> CaloriesDetailContent(state)
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun ReadinessDetailContent(state: HealthDashboardState) {
    val score = state.readinessScore
    val readinessLevel = state.readinessLevel
    val nightsRemaining = state.readinessNightsRemaining
    val nightsText = if (nightsRemaining == 1) "1 more night" else "$nightsRemaining more nights"
    val bannerText = when {
        score != null -> "RECOVERY LEVEL · ${readinessLevel?.uppercase() ?: "OPTIMAL"}"
        readinessLevel == "Incomplete Data" -> "INCOMPLETE DATA"
        else -> "CALIBRATING · $nightsRemaining MORE NIGHT${if (nightsRemaining == 1) "" else "S"} NEEDED"
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Circular Ring
                BaroCircleGauge(
                    label = "READINESS*",
                    valueText = score?.toString() ?: "—",
                    progress = score?.let { it / 100f },
                    accentColor = BaroReadinessTeal,
                    size = 110.dp,
                    strokeWidth = 10.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Vayu recovery estimate*",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BaroReadinessTeal
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF222228))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (score != null) BaroReadinessTeal else BaroZone3Orange)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bannerText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (score != null) BaroReadinessTeal else BaroZone3Orange,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Readiness Calibration Notice / Summary
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                val header = when {
                    score != null -> "READINESS RECOVERY ASSESSMENT"
                    readinessLevel == "Incomplete Data" -> "READINESS INCOMPLETE DATA"
                    else -> "READINESS CALIBRATION"
                }
                Text(header, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                val mainText = state.readinessSummaryText ?: if (score != null) {
                    "Readiness is available."
                } else {
                    "To complete baseline calibration, wear your device to sleep for $nightsText."
                }
                Text(
                    text = mainText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BaroTextWhite,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                val subText = if (score != null) {
                    "Vayu fixed-reference model using recorded sleep, HRV, and resting heart rate."
                } else if (readinessLevel == "Incomplete Data") {
                    "No score is shown until every required record is available for today."
                } else {
                    "Vayu analyzes 7 distinct overnight sleep recordings to establish your baseline calibration without synthetic fallback data."
                }
                Text(
                    text = subText,
                    fontSize = 12.sp,
                    color = BaroTextMuted,
                    lineHeight = 17.sp
                )
            }
        }

        // Vayu Model Transparency Card
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = BaroReadinessTeal, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("VAYU READINESS MODEL*", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Calculated on-device from your authentic Health Connect sleep, HRV, and resting heart rate data. Android Health Connect and the Google Health API do not export proprietary Readiness scores to third-party apps. A future direct API would require a Vayu update before its score could be displayed.",
                    fontSize = 11.5.sp,
                    color = BaroTextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        // Inputs used by the Vayu readiness model
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("WHAT IMPACTS READINESS (RECOVERY)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(14.dp))

                FactorItem("Sleep Duration & Need", "Total restful sleep logged vs your nightly sleep need.")
                Spacer(modifier = Modifier.height(10.dp))
                FactorItem("Sleep Architecture", "Proportion of restorative Deep (SWS) and REM sleep stages.")
                Spacer(modifier = Modifier.height(10.dp))
                FactorItem("Resting Heart Rate", "Recorded resting pulse evaluated by Vayu's fixed-reference model.")
                Spacer(modifier = Modifier.height(10.dp))
                FactorItem("Heart Rate Variability (HRV)", "RMSSD nocturnal autonomic nervous system balance.")
            }
        }

        // Recovery Zones
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("VAYU READINESS TIERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(12.dp))
                ZoneDescRow("65–100%", "High (Green)", "Primed for high strain, exertion, and peak training.", BaroZone1Green)
                Spacer(modifier = Modifier.height(8.dp))
                ZoneDescRow("30–64%", "Moderate (Navy)", "Sufficient recovery. Maintain regular training load.", BaroZone2Blue)
                Spacer(modifier = Modifier.height(8.dp))
                ZoneDescRow("1–29%", "Low (Orange)", "Body is recovering from fatigue, illness, or strain. Rest suggested.", BaroZone3Orange)
            }
        }
    }
}

@Composable
private fun HeartRateDetailContent(state: HealthDashboardState) {
    val hr = state.heartRate
    val rhr = state.restingHeartRate
    val minBpm = hr?.minBpm
    val maxBpm = hr?.maxBpm
    val latestBpm = hr?.latestBpm ?: rhr?.beatsPerMinute
    val samples = hr?.samples?.map { it.beatsPerMinute } ?: emptyList()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HEART RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                    if (rhr != null) {
                        Text("REST ${rhr.beatsPerMinute} BPM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroReadinessTeal)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val rangeText = if (minBpm != null && maxBpm != null) "$minBpm–$maxBpm bpm" else if (latestBpm != null) "$latestBpm bpm" else "—"
                Text(text = rangeText, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)

                Spacer(modifier = Modifier.height(16.dp))

                BaroSparklineWave(
                    samples = samples,
                    height = 96.dp,
                    showLiveIndicator = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("12:00 AM", fontSize = 9.sp, color = BaroTextMuted)
                    Text("6:00 AM", fontSize = 9.sp, color = BaroTextMuted)
                    Text("12:00 PM", fontSize = 9.sp, color = BaroTextMuted)
                    Text("6:00 PM", fontSize = 9.sp, color = BaroTextMuted)
                }
            }
        }

        // Time in Zones
        val zoneBreakdown = com.vayu.health.data.model.HealthDataCalculations.calculateHeartRateZones(
            samples = hr?.samples ?: emptyList(),
            restingHr = state.restingHeartRate?.beatsPerMinute?.toInt()
        )
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("TIME IN ZONES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(12.dp))
                val z4Text = if (zoneBreakdown.totalMinutes > 0) "${zoneBreakdown.zone4Minutes} min" else "—"
                ZoneBar("Zone 4 · Peak (${zoneBreakdown.zone4Label})", z4Text, zoneBreakdown.zone4Fraction, BaroZone4Red)
                Spacer(modifier = Modifier.height(8.dp))
                val z3Text = if (zoneBreakdown.totalMinutes > 0) "${zoneBreakdown.zone3Minutes} min" else "—"
                ZoneBar("Zone 3 · Cardio (${zoneBreakdown.zone3Label})", z3Text, zoneBreakdown.zone3Fraction, BaroZone3Orange)
                Spacer(modifier = Modifier.height(8.dp))
                val z2Text = if (zoneBreakdown.totalMinutes > 0) "${zoneBreakdown.zone2Minutes} min" else "—"
                ZoneBar("Zone 2 · Fat Burn (${zoneBreakdown.zone2Label})", z2Text, zoneBreakdown.zone2Fraction, BaroZone2Blue)
                Spacer(modifier = Modifier.height(8.dp))
                val z1Text = if (zoneBreakdown.totalMinutes > 0) "${zoneBreakdown.zone1Minutes} min" else "—"
                ZoneBar("Zone 1 · Below / Warm up (${zoneBreakdown.zone1Label})", z1Text, zoneBreakdown.zone1Fraction, BaroZone1Green)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Calculated from timestamped sample intervals and individualized Heart Rate Reserve (Karvonen method).",
                    fontSize = 11.sp,
                    color = BaroTextMuted,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun StepsDetailContent(state: HealthDashboardState) {
    val act = state.activity
    val steps = act?.steps
    val stepsFormatted = steps?.let { "%,d".format(it) } ?: "—"
    val distKm = act?.distanceMeters?.let { "%.2f km".format(it / 1000.0) } ?: "—"
    val cal = act?.totalCaloriesKcal?.let { "%.0f kcal".format(it) } ?: "—"

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("TOTAL STEPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stepsFormatted, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailStat("DISTANCE", distKm)
                    DetailStat("TOTAL ENERGY", cal)
                    DetailStat("ACTIVE ENERGY", act?.activeCaloriesKcal?.let { "%.0f kcal".format(it) } ?: "—")
                }
            }
        }
    }
}

@Composable
private fun RestingHrDetailContent(state: HealthDashboardState) {
    val rhr = state.restingHeartRate?.beatsPerMinute
    val history = state.dailyRestingHrHistory.map { it.first to it.second.toDouble() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("RESTING HEART RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(rhr?.let { "$it bpm" } ?: "—", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                val rhrStatus = if (rhr != null) "Resting heart rate recorded by Fitbit" else "Data unavailable"
                Text(rhrStatus, fontSize = 12.sp, color = if (rhr != null) BaroReadinessTeal else BaroTextMuted)
            }
        }

        Vitals7DayBarCard(
            title = "RESTING HEART RATE",
            history = history,
            unit = "bpm",
            accentColor = BaroReadinessTeal,
            formatValue = { "%.0f".format(it) }
        )
    }
}

@Composable
private fun HrvDetailContent(state: HealthDashboardState) {
    val hrv = state.hrv?.rmssdMillis
    val history = state.dailyHrvHistory
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("HEART RATE VARIABILITY (HRV)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(hrv?.let { "%.0f ms".format(it) } ?: "—", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                val hrvStatus = if (hrv != null) "Building baseline · Multi-day nocturnal tracking" else "Data unavailable"
                Text(hrvStatus, fontSize = 12.sp, color = BaroTextMuted)
            }
        }

        Vitals7DayBarCard(
            title = "HRV (RMSSD)",
            history = history,
            unit = "ms",
            accentColor = Color(0xFF8A70D6),
            formatValue = { "%.0f".format(it) }
        )
    }
}

@Composable
private fun BreathingRateDetailContent(state: HealthDashboardState) {
    val resp = state.respiratory?.breathsPerMinute
    val history = state.respiratory?.dailyBreathingRateHistory ?: emptyList()
    val isFallback = state.respiratory?.isBreathingRateHistoricalFallback == true
    val timestamp = state.respiratory?.respiratoryTimestamp
    val timeFormatted = formatDetailTimestamp(timestamp)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("BREATHING RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(resp?.let { "%.1f brpm".format(it) } ?: "—", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                val respStatus = if (resp != null) {
                    if (isFallback && timeFormatted.isNotEmpty()) {
                        "Most recent recorded night ($timeFormatted) · Normal sleeping range"
                    } else if (timeFormatted.isNotEmpty()) {
                        "Recorded $timeFormatted · Normal sleeping range"
                    } else {
                        "✓ Typical · In normal sleeping range"
                    }
                } else "Data unavailable"
                Text(respStatus, fontSize = 12.sp, color = if (resp != null) BaroReadinessTeal else BaroTextMuted)
            }
        }

        Vitals7DayBarCard(
            title = "BREATHING RATE",
            history = history,
            unit = "brpm",
            accentColor = BaroStrainBlue,
            formatValue = { "%.1f".format(it) }
        )
    }
}

@Composable
private fun Spo2DetailContent(state: HealthDashboardState) {
    val rawSpo2 = state.respiratory?.spo2Percentage
    val spo2 = rawSpo2?.let { if (it <= 1.0) it * 100.0 else it }
    val history = state.respiratory?.dailySpo2History ?: emptyList()
    val isFallback = state.respiratory?.isSpo2HistoricalFallback == true
    val timestamp = state.respiratory?.spo2Timestamp
    val timeFormatted = formatDetailTimestamp(timestamp)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("SpO2 (BLOOD OXYGEN)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(spo2?.let { "%.0f%%".format(it) } ?: "—", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                val spo2Status = if (spo2 != null) {
                    if (isFallback && timeFormatted.isNotEmpty()) {
                        "Most recent recorded night ($timeFormatted) · Normal blood oxygen level"
                    } else if (timeFormatted.isNotEmpty()) {
                        "Recorded $timeFormatted · Normal blood oxygen level"
                    } else {
                        "✓ Typical · Normal blood oxygen level"
                    }
                } else "Data unavailable"
                Text(spo2Status, fontSize = 12.sp, color = if (spo2 != null) BaroReadinessTeal else BaroTextMuted)
            }
        }

        Vitals7DayBarCard(
            title = "SpO2",
            history = history,
            unit = "%",
            accentColor = BaroReadinessTeal,
            formatValue = { "%.0f".format(it) }
        )
    }
}

@Composable
private fun SkinTempDetailContent(state: HealthDashboardState) {
    val delta = state.respiratory?.skinTemperature?.averageDeltaCelsius
    val history = state.respiratory?.dailySkinTempHistory ?: emptyList()
    val timestamp = state.respiratory?.skinTemperature?.timestamp
    val timeFormatted = formatDetailTimestamp(timestamp)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("SKIN TEMPERATURE VARIATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(delta?.let { "%+.1f °C".format(it) } ?: "—", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                val skinStatus = if (delta != null) {
                    if (timeFormatted.isNotEmpty()) "Recorded $timeFormatted · Multi-day nocturnal tracking" else "Building baseline · Multi-day nocturnal tracking"
                } else "Data unavailable"
                Text(skinStatus, fontSize = 12.sp, color = BaroTextMuted)
            }
        }

        Vitals7DayBarCard(
            title = "SKIN TEMP VARIATION",
            history = history,
            unit = "°C",
            accentColor = BaroHeartCoral,
            formatValue = { "%+.1f".format(it) }
        )
    }
}

@Composable
private fun Vitals7DayBarCard(
    title: String,
    history: List<Pair<java.time.LocalDate, Double>>,
    unit: String,
    accentColor: Color = BaroReadinessTeal,
    formatValue: (Double) -> String = { "%.1f".format(it) }
) {
    val today = java.time.LocalDate.now()
    val last7Days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val historyMap = history.toMap()
    val values = last7Days.map { date -> historyMap[date] }
    val validValues = values.filterNotNull()
    val avg = if (validValues.isNotEmpty()) validValues.average() else null
    val min = validValues.minOrNull()
    val max = validValues.maxOrNull()

    BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$title · PAST 7 DAYS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextSecondary,
                    letterSpacing = 0.8.sp
                )
                if (avg != null) {
                    Text(
                        text = "AVG ${formatValue(avg)} $unit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val floatValues = values.map { it?.toFloat() }
            val dayLabels = last7Days.map { it.dayOfWeek.name.take(1) }
            BaroWeeklyBarChart(
                dailyNullableValues = floatValues,
                labels = dayLabels,
                barColor = accentColor,
                todayIndex = 6,
                height = 54.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val rangeStr = if (min != null && max != null) {
                    "${formatValue(min)}–${formatValue(max)} $unit"
                } else "Collecting history"
                Text(
                    text = "7-day range: $rangeStr",
                    fontSize = 11.sp,
                    color = BaroTextMuted
                )
                Text(
                    text = "${validValues.size}/7 days recorded",
                    fontSize = 11.sp,
                    color = if (validValues.size >= 5) BaroReadinessTeal else BaroZone3Orange
                )
            }
        }
    }
}

@Composable
private fun ZoneMinutesDetailContent(state: HealthDashboardState) {
    val hr = state.heartRate
    val rhr = state.restingHeartRate
    val zoneBreakdown = com.vayu.health.data.model.HealthDataCalculations.calculateHeartRateZones(
        samples = hr?.samples ?: emptyList(),
        restingHr = rhr?.beatsPerMinute?.toInt()
    )
    val hasData = !hr?.samples.isNullOrEmpty()
    val zoneMin = if (hasData) zoneBreakdown.activeZoneMinutes.toInt() else null

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("ZONE MINUTES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(zoneMin?.let { "$it min" } ?: "—", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                val subtitle = if (zoneMin != null) {
                    "Active duration across Fat Burn (1x), Cardio (2x), and Peak (2x) zones based on individualized Heart Rate Reserve."
                } else {
                    "Data unavailable (no continuous pulse samples)"
                }
                Text(subtitle, fontSize = 12.sp, color = BaroTextMuted, lineHeight = 16.sp)
            }
        }

        if (hasData) {
            BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
                Column {
                    Text("ZONE BREAKDOWN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    ZoneBar("Zone 4 · Peak (${zoneBreakdown.zone4Label})", "${zoneBreakdown.zone4Minutes} min", zoneBreakdown.zone4Fraction, BaroZone4Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    ZoneBar("Zone 3 · Cardio (${zoneBreakdown.zone3Label})", "${zoneBreakdown.zone3Minutes} min", zoneBreakdown.zone3Fraction, BaroZone3Orange)
                    Spacer(modifier = Modifier.height(8.dp))
                    ZoneBar("Zone 2 · Fat Burn (${zoneBreakdown.zone2Label})", "${zoneBreakdown.zone2Minutes} min", zoneBreakdown.zone2Fraction, BaroZone2Blue)
                }
            }
        }
    }
}

@Composable
private fun StressDetailContent(state: HealthDashboardState) {
    val hr = state.heartRate
    val rhr = state.restingHeartRate
    val hrv = state.hrv
    val stress = HealthDataCalculations.calculatePhysiologicalStress(hr, rhr, hrv)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("STRESS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                val scoreStr = stress?.let { s -> "%.1f AVG · %s".format(s.averageScore, s.levelLabel) } ?: "Data unavailable"
                Text(scoreStr, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(14.dp))
                BaroSpeedometer(score = stress?.averageScore, height = 72.dp)
                Spacer(modifier = Modifier.height(10.dp))
                val hiLo = stress?.let { s -> "HI %.1f   LO %.1f".format(s.maxScore, s.minScore) } ?: ""
                if (hiLo.isNotEmpty()) {
                    Text(hiLo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    "Personal indicator estimating autonomic balance from resting heart rate elevation and nocturnal HRV (RMSSD) suppression.",
                    fontSize = 11.sp,
                    color = BaroTextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        // Stress Zones Guide
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("AUTONOMIC TIERS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                ZoneDescRow("0.0 – 1.0", "Calm", "Resting pulse near baseline with high nocturnal HRV.", BaroReadinessTeal)
                ZoneDescRow("1.0 – 2.0", "Low Stress", "Normal daily cognitive engagement and relaxed movement.", BaroZone2Blue)
                ZoneDescRow("2.0 – 2.5", "Medium Stress", "Mild sympathetic activation, cognitive load, or physical fatigue.", BaroZone3Orange)
                ZoneDescRow("2.5 – 4.0", "High Stress", "Elevated resting heart rate or suppressed nocturnal HRV.", BaroHeartCoral)
            }
        }
    }
}

@Composable
private fun FactorItem(title: String, desc: String) {
    Column {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BaroTextWhite)
        Spacer(modifier = Modifier.height(2.dp))
        Text(desc, fontSize = 11.sp, color = BaroTextMuted, lineHeight = 16.sp)
    }
}

@Composable
private fun ZoneDescRow(range: String, title: String, desc: String, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text("$range · $title", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
            Text(desc, fontSize = 11.sp, color = BaroTextMuted, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun ZoneBar(name: String, duration: String, fraction: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontSize = 11.sp, color = BaroTextSecondary)
            Text(duration, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF222226))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(fraction.coerceIn(0.01f, 1f))
                    .background(color)
            )
            if (fraction < 1f) {
                Spacer(modifier = Modifier.weight(1f - fraction))
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Column {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BaroTextMuted, letterSpacing = 0.6.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
    }
}

@Composable
private fun ResilienceDetailContent(state: HealthDashboardState) {
    val resilience = state.resilience ?: HealthDataCalculations.calculateResilience(
        state.sleep,
        state.activity,
        state.hrv,
        state.restingHeartRate
    )
    val resiliencePurple = Color(0xFF8A70D6)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Hero Card
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Resilience*",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BaroTextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resilience.levelLabel,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = resiliencePurple
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = resilience.summaryText,
                    fontSize = 13.sp,
                    color = BaroTextWhite,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            }
        }

        // Vayu Model Transparency Card
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = resiliencePurple, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("VAYU RESILIENCE MODEL*", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Resilience* is modeled on-device by Vayu from your authentic Health Connect sleep, activity, resting heart rate, and HRV records. Android Health Connect and the Google Health API do not export proprietary Resilience scores to third-party applications. Vayu evaluates recovery across three physiological pillars without cloud telemetry.",
                    fontSize = 11.5.sp,
                    color = BaroTextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        // Section Title
        Text(
            text = "PILLARS OF RESILIENCE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BaroTextSecondary,
            letterSpacing = 0.8.sp
        )

        // Pillar 1: Sleep patterns
        ResiliencePillarCard(
            title = "Sleep patterns",
            status = resilience.sleepPatternsStatus,
            description = resilience.sleepPatternsText,
            accentColor = BaroSleepBlue
        )

        // Pillar 2: Activity balance
        ResiliencePillarCard(
            title = "Activity balance",
            status = resilience.activityBalanceStatus,
            description = resilience.activityBalanceText,
            accentColor = BaroStrainBlue
        )

        // Pillar 3: Physical calmness
        ResiliencePillarCard(
            title = "Physical calmness",
            status = resilience.physicalCalmnessStatus,
            description = resilience.physicalCalmnessText,
            accentColor = resiliencePurple
        )
    }
}

@Composable
private fun ResiliencePillarCard(
    title: String,
    status: String,
    description: String,
    accentColor: Color
) {
    BaroCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextWhite
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 12.5.sp,
                color = BaroTextMuted,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun CaloriesDetailContent(state: HealthDashboardState) {
    val totalCal = state.activity?.totalCaloriesKcal?.toInt()
        ?: state.activity?.activeCaloriesKcal?.toInt()
    val activeCal = state.activity?.activeCaloriesKcal?.toInt()
    val basalCal = if (totalCal != null && activeCal != null) (totalCal - activeCal).coerceAtLeast(0) else null
    val distanceKm = state.activity?.distanceMeters?.let { "%.2f km".format(it / 1000.0) } ?: "—"
    val stepsStr = state.activity?.steps?.let { "%,d".format(it) } ?: "—"
    val hrZones = HealthDataCalculations.calculateHeartRateZones(
        samples = state.heartRate?.samples ?: emptyList(),
        restingHr = state.restingHeartRate?.beatsPerMinute?.toInt()
    )
    val zoneMins = if (state.heartRate?.samples.isNullOrEmpty()) null else hrZones.activeZoneMinutes.toInt()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Hero Card
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOTAL ENERGY BURNED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BaroTextSecondary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = totalCal?.toString() ?: "—",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = BaroTextWhite
                    )
                    if (totalCal != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "kcal",
                            fontSize = 14.sp,
                            color = BaroTextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Total energy expenditure calculated from your basal metabolic rate and physical activity.",
                    fontSize = 11.5.sp,
                    color = BaroTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }

        // Breakdown Card
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("ENERGY BREAKDOWN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Burn", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BaroTextWhite)
                        Text("Calories burned through physical movement", fontSize = 10.5.sp, color = BaroTextMuted)
                    }
                    Text(activeCal?.let { "$it kcal" } ?: "—", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BaroReadinessTeal)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Basal Metabolic Rate (BMR)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BaroTextWhite)
                        Text("Calories burned while at rest", fontSize = 10.5.sp, color = BaroTextMuted)
                    }
                    Text(basalCal?.let { "$it kcal" } ?: "—", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                }
            }
        }

        // Movement Stats
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                DetailStat(label = "STEPS", value = stepsStr)
                DetailStat(label = "DISTANCE", value = distanceKm)
                DetailStat(label = "ACTIVE MINS", value = zoneMins?.let { "$it min" } ?: "—")
            }
        }
    }
}

private fun formatDetailTimestamp(instant: java.time.Instant?): String {
    if (instant == null) return ""
    val dtf = java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a").withZone(java.time.ZoneId.systemDefault())
    return dtf.format(instant)
}

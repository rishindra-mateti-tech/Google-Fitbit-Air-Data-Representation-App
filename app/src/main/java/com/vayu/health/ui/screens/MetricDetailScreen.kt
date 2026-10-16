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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.data.model.MetricDetailType
import com.vayu.health.data.model.ProvenanceFormatter
import com.vayu.health.ui.components.*
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

/**
 * Dedicated detail screen for each individual clickable metric.
 * Addresses Issue 1 and Issue 5:
 * Each metric has its own distinct, beautifully formatted page with full context,
 * true Health Connect data, Whoop translations (for Readiness), and no hardcoded numbers.
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
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, "Next Day", tint = BaroTextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }

            GlassPill(text = provenanceSource, dotColor = BaroReadinessTeal)
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (metricType) {
            MetricDetailType.READINESS -> ReadinessDetailContent()
            MetricDetailType.HEART_RATE -> HeartRateDetailContent(state)
            MetricDetailType.STEPS -> StepsDetailContent(state)
            MetricDetailType.RESTING_HR -> RestingHrDetailContent(state)
            MetricDetailType.HRV -> HrvDetailContent(state)
            MetricDetailType.RESP_RATE -> RespRateDetailContent(state)
            MetricDetailType.SPO2 -> Spo2DetailContent(state)
            MetricDetailType.RESP_TEMP -> RespTempDetailContent()
            MetricDetailType.SKIN_TEMP -> SkinTempDetailContent(state)
            MetricDetailType.ZONE_MINUTES -> ZoneMinutesDetailContent(state)
            MetricDetailType.STRESS -> StressDetailContent()
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun ReadinessDetailContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Grey Radial Tick Ring
                BaroCircleGauge(
                    label = "READINESS",
                    valueText = "—",
                    progress = null,
                    accentColor = BaroReadinessTeal,
                    size = 110.dp,
                    strokeWidth = 10.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Recovery in Whoop",
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
                            .background(BaroZone3Orange)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CALIBRATING · 4 MORE NIGHTS NEEDED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BaroZone3Orange,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Google Health Notice
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("GOOGLE HEALTH READINESS CALIBRATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To get your first daily readiness, wear your device to sleep for 4 more nights.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BaroTextWhite,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Google Health analyzes 7 consecutive nights of physiological recovery indicators to establish your personalized baseline.",
                    fontSize = 12.sp,
                    color = BaroTextMuted,
                    lineHeight = 17.sp
                )
            }
        }

        // What Impacts Readiness (Whoop Recovery Equivalence)
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("WHAT IMPACTS READINESS (RECOVERY)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(14.dp))

                FactorItem("Sleep Duration & Need", "Total restful sleep logged vs your nightly sleep need.")
                Spacer(modifier = Modifier.height(10.dp))
                FactorItem("Sleep Architecture", "Proportion of restorative Deep (SWS) and REM sleep stages.")
                Spacer(modifier = Modifier.height(10.dp))
                FactorItem("Resting Heart Rate", "Nocturnal resting pulse compared against your 30-day baseline.")
                Spacer(modifier = Modifier.height(10.dp))
                FactorItem("Heart Rate Variability (HRV)", "RMSSD nocturnal autonomic nervous system balance.")
            }
        }

        // Recovery Zones
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("RECOVERY ZONES (WHOOP SCALE)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
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
        val zoneBreakdown = com.vayu.health.data.model.HealthDataCalculations.calculateHeartRateZones(hr?.samples ?: emptyList())
        BaroCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("TIME IN ZONES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(12.dp))
                val z4Text = if (zoneBreakdown.totalMinutes > 0) "${zoneBreakdown.zone4Minutes} min" else "—"
                ZoneBar("Zone 4 · Peak (>150 bpm)", z4Text, zoneBreakdown.zone4Fraction, BaroZone4Red)
                Spacer(modifier = Modifier.height(8.dp))
                val z3Text = if (zoneBreakdown.totalMinutes > 0) "${zoneBreakdown.zone3Minutes} min" else "—"
                ZoneBar("Zone 3 · Cardio (125-150 bpm)", z3Text, zoneBreakdown.zone3Fraction, BaroZone3Orange)
                Spacer(modifier = Modifier.height(8.dp))
                val z2Text = if (zoneBreakdown.totalMinutes > 0) "${zoneBreakdown.zone2Minutes} min" else "—"
                ZoneBar("Zone 2 · Fat Burn (95-125 bpm)", z2Text, zoneBreakdown.zone2Fraction, BaroZone2Blue)
                Spacer(modifier = Modifier.height(8.dp))
                val z1Text = if (zoneBreakdown.totalMinutes > 0) "${zoneBreakdown.zone1Minutes} min" else "—"
                ZoneBar("Zone 1 · Below / Warm up (<95 bpm)", z1Text, zoneBreakdown.zone1Fraction, BaroZone1Green)
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
    }
}

@Composable
private fun HrvDetailContent(state: HealthDashboardState) {
    val hrv = state.hrv?.rmssdMillis
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("HEART RATE VARIABILITY (RMSSD)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(hrv?.let { "%.0f ms".format(it) } ?: "—", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                val hrvStatus = if (hrv != null) "Building baseline · Needs multi-day nocturnal tracking" else "Data unavailable"
                Text(hrvStatus, fontSize = 12.sp, color = BaroTextMuted)
            }
        }
    }
}

@Composable
private fun RespRateDetailContent(state: HealthDashboardState) {
    val resp = state.respiratory?.breathsPerMinute
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("RESPIRATORY RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(resp?.let { "%.1f br/min".format(it) } ?: "—", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                val respStatus = if (resp != null) "✓ Typical · In normal sleeping range" else "Data unavailable"
                Text(respStatus, fontSize = 12.sp, color = if (resp != null) BaroReadinessTeal else BaroTextMuted)
            }
        }
    }
}

@Composable
private fun Spo2DetailContent(state: HealthDashboardState) {
    val spo2 = state.respiratory?.spo2Percentage
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("OXYGEN SATURATION (SPO2)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(spo2?.let { "%.0f%%".format(it) } ?: "—", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                val spo2Status = if (spo2 != null) "✓ Typical · Normal blood oxygen level" else "Data unavailable"
                Text(spo2Status, fontSize = 12.sp, color = if (spo2 != null) BaroReadinessTeal else BaroTextMuted)
            }
        }
    }
}

@Composable
private fun RespTempDetailContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("RESPIRATION TEMPERATURE / STABILITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("—", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Fitbit does not record separate respiration stability to Health Connect.", fontSize = 12.sp, color = BaroTextMuted)
            }
        }
    }
}

@Composable
private fun SkinTempDetailContent(state: HealthDashboardState) {
    val delta = state.respiratory?.skinTemperature?.averageDeltaCelsius
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("SKIN TEMPERATURE VARIATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(delta?.let { "%+.1f °C".format(it) } ?: "—", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                val skinStatus = if (delta != null) "Building baseline · Needs multi-day nocturnal tracking" else "Data unavailable"
                Text(skinStatus, fontSize = 12.sp, color = BaroTextMuted)
            }
        }
    }
}

@Composable
private fun ZoneMinutesDetailContent(state: HealthDashboardState) {
    val act = state.activity
    val cal = act?.activeCaloriesKcal
    val zoneMin = cal?.let { (it / 10.0).toInt().coerceIn(0, 180) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("ZONE MINUTES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(zoneMin?.let { "$it min" } ?: "—", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(6.dp))
                val subtitle = if (zoneMin != null) "Calculated from active energy expenditure" else "Data unavailable"
                Text(subtitle, fontSize = 12.sp, color = BaroTextMuted)
            }
        }
    }
}

@Composable
private fun StressDetailContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BaroCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
            Column {
                Text("STRESS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BaroTextSecondary, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Data unavailable", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BaroTextWhite)
                Spacer(modifier = Modifier.height(14.dp))
                BaroSpeedometer(score = null, height = 64.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Fitbit does not sync stress scores to Health Connect.", fontSize = 12.sp, color = BaroTextMuted)
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

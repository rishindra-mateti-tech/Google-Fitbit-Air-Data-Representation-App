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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.data.model.ProvenanceFormatter
import com.vayu.health.ui.components.GlassPill
import com.vayu.health.ui.components.VayuTab
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

/** Reference-driven overview: compact, restrained and strictly source-faithful. */
@Composable
fun BaroOverviewScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    onOpenTab: (VayuTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = state.activity
    val source = ProvenanceFormatter.formatCompact(state.selectedSource)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            GlassPill(text = source, dotColor = VayuCyan)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Today", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(viewModel.getFormattedDate(), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextMuted)
            }
            IconButton(onClick = { viewModel.loadMetrics(forceRefresh = true) }) {
                Icon(Icons.Default.Refresh, "Refresh Health Connect data", tint = TextPrimary, modifier = Modifier.size(21.dp))
            }
        }
        Spacer(Modifier.height(30.dp))

        SectionHeading("ACTIVITY", "Tap to explore") { onOpenTab(VayuTab.ACTIVITY) }
        Spacer(Modifier.height(10.dp))
        ActivitySummaryCard(activity?.steps, activity?.totalCaloriesKcal, activity?.distanceMeters) { onOpenTab(VayuTab.ACTIVITY) }

        Spacer(Modifier.height(28.dp))
        SectionHeading("PINNED", "Health Connect records")
        Spacer(Modifier.height(6.dp))
        Text("Today", fontSize = 13.sp, color = TextMuted, modifier = Modifier.padding(bottom = 14.dp))

        ReferenceMetricGrid(state, onOpenTab)

        Spacer(Modifier.height(26.dp))
        Text(
            "Every number is an original Health Connect record. Vayu does not add scores, estimates, trends, or medical interpretation.",
            fontSize = 11.sp, lineHeight = 16.sp, color = TextMuted, modifier = Modifier.padding(horizontal = 2.dp)
        )
        Spacer(Modifier.height(92.dp))
    }
}

@Composable
private fun ReferenceMetricGrid(state: HealthDashboardState, onOpenTab: (VayuTab) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ReferenceMetricCard("Resting HR", state.restingHeartRate?.beatsPerMinute?.toString(), "bpm", Icons.Default.Favorite, HeartCrimson, { onOpenTab(VayuTab.VITALS) }, Modifier.weight(1f))
        ReferenceMetricCard("HRV", state.hrv?.let { "%.1f".format(it.rmssdMillis) }, "ms", Icons.Default.Timeline, HrvEmerald, { onOpenTab(VayuTab.VITALS) }, Modifier.weight(1f))
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ReferenceMetricCard("Resp. rate", state.respiratory?.breathsPerMinute?.let { "%.1f".format(it) }, "br/min", Icons.Default.Timeline, RespirationTeal, { onOpenTab(VayuTab.VITALS) }, Modifier.weight(1f))
        ReferenceMetricCard("SpO2", state.respiratory?.spo2Percentage?.let { "%.1f".format(it) }, "%", Icons.Default.Timeline, Spo2Blue, { onOpenTab(VayuTab.VITALS) }, Modifier.weight(1f))
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ReferenceMetricCard("Sleep", state.sleep?.actualSleepDurationMinutes?.let { "${it / 60}h ${it % 60}m" }, null, Icons.Default.Bedtime, SleepRem, { onOpenTab(VayuTab.SLEEP) }, Modifier.weight(1f))
        ReferenceMetricCard("Heart rate", state.heartRate?.latestBpm?.toString(), "bpm", Icons.Default.Favorite, HeartCrimson, { onOpenTab(VayuTab.VITALS) }, Modifier.weight(1f))
    }
}

@Composable
private fun SectionHeading(title: String, action: String, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(action, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextMuted, modifier = Modifier.clickable(onClick = onClick).padding(4.dp))
    }
}

@Composable
private fun ActivitySummaryCard(steps: Long?, energy: Double?, distanceMeters: Double?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(25.dp)
    Column(
        modifier = Modifier.fillMaxWidth().clip(shape).background(Color(0xFF0C0E13)).border(1.dp, Color(0x25FFFFFF), shape).clickable(onClick = onClick).padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(VayuCyan.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = VayuCyan, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("TODAY'S MOVEMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp, color = TextSecondary)
                Text("Original Health Connect totals", fontSize = 11.sp, color = TextMuted)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ActivityValue("STEPS", steps?.let { "%,d".format(it) } ?: "—", VayuCyan)
            ActivityValue("ENERGY", energy?.let { "%.0f kcal".format(it) } ?: "—", EnergyAmber)
            ActivityValue("DISTANCE", distanceMeters?.let { "%.2f km".format(it / 1000) } ?: "—", TextPrimary)
        }
    }
}

@Composable
private fun ActivityValue(label: String, value: String, accent: Color) {
    Column {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp, color = TextMuted)
        Spacer(Modifier.height(5.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent, maxLines = 1)
    }
}

@Composable
private fun ReferenceMetricCard(title: String, value: String?, unit: String?, icon: ImageVector, accent: Color, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier.height(150.dp).clip(shape).background(Color(0xFF0B0D12)).border(1.dp, Color(0x22FFFFFF), shape).clickable(onClick = onClick).padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, tint = accent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.55.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(17.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value ?: "—", fontSize = if (value == null) 34.sp else 29.sp, fontWeight = FontWeight.Medium, color = if (value == null) TextUnavailable else TextPrimary, maxLines = 1)
            if (value != null && unit != null) {
                Spacer(Modifier.width(4.dp))
                Text(unit, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextMuted, modifier = Modifier.padding(bottom = 5.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Text(if (value == null) "No record today" else "Tap to explore", fontSize = 10.sp, color = if (value == null) TextMuted else accent)
    }
}

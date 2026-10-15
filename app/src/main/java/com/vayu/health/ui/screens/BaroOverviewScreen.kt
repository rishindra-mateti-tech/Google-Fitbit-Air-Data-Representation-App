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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.vayu.health.ui.theme.CardBackgroundGlass
import com.vayu.health.ui.theme.EnergyAmber
import com.vayu.health.ui.theme.GlassBorder
import com.vayu.health.ui.theme.HeartCrimson
import com.vayu.health.ui.theme.HrvEmerald
import com.vayu.health.ui.theme.RespirationTeal
import com.vayu.health.ui.theme.SleepRem
import com.vayu.health.ui.theme.Spo2Blue
import com.vayu.health.ui.theme.TextMuted
import com.vayu.health.ui.theme.TextPrimary
import com.vayu.health.ui.theme.TextUnavailable
import com.vayu.health.ui.theme.VayuCyan
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

/**
 * A compact, Baro-inspired overview. It deliberately renders only raw Health
 * Connect values; score rings, readiness, strain and baselines are excluded.
 */
@Composable
fun BaroOverviewScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    onOpenTab: (VayuTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = state.activity
    val sleep = state.sleep
    val scroll = rememberScrollState()
    val sourceLabel = ProvenanceFormatter.formatCompact(state.selectedSource)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("TODAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.2.sp)
                Text("Your health", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, letterSpacing = (-0.7).sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassPill(text = sourceLabel, dotColor = VayuCyan)
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { viewModel.loadMetrics(forceRefresh = true) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Health Connect data", tint = TextPrimary)
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = viewModel.getFormattedDate(),
            fontSize = 13.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(18.dp))

        Text("TODAY AT A GLANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HealthMetricCard(
                title = "Actual sleep",
                value = sleep?.actualSleepDurationMinutes?.let { "${it / 60}h ${it % 60}m" },
                subtitle = if (sleep != null) "Recorded sleep session" else "No sleep session today",
                icon = Icons.Default.Bedtime,
                accent = SleepRem,
                onClick = { onOpenTab(VayuTab.SLEEP) },
                modifier = Modifier.weight(1f)
            )
            HealthMetricCard(
                title = "Heart rate",
                value = state.heartRate?.latestBpm?.toString(),
                unit = "bpm",
                subtitle = if (state.heartRate?.latestBpm != null) "Latest recorded sample" else "No samples today",
                icon = Icons.Default.Favorite,
                accent = HeartCrimson,
                onClick = { onOpenTab(VayuTab.VITALS) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("ACTIVITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HealthMetricCard(
                title = "Steps",
                value = activity?.steps?.let { "%,d".format(it) },
                subtitle = if (activity?.steps != null) "Health Connect total" else "No steps today",
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                accent = VayuCyan,
                onClick = { onOpenTab(VayuTab.ACTIVITY) },
                modifier = Modifier.weight(1f)
            )
            HealthMetricCard(
                title = "Total energy",
                value = activity?.totalCaloriesKcal?.let { "%.0f".format(it) },
                unit = "kcal",
                subtitle = if (activity?.totalCaloriesKcal != null) "Health Connect total" else "No energy record",
                icon = Icons.Default.LocalFireDepartment,
                accent = EnergyAmber,
                onClick = { onOpenTab(VayuTab.ACTIVITY) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("VITALS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HealthMetricCard(
                title = "Resting HR",
                value = state.restingHeartRate?.beatsPerMinute?.toString(),
                unit = "bpm",
                subtitle = if (state.restingHeartRate != null) "Original record" else "No record today",
                icon = Icons.Default.Favorite,
                accent = HeartCrimson,
                onClick = { onOpenTab(VayuTab.VITALS) },
                modifier = Modifier.weight(1f)
            )
            HealthMetricCard(
                title = "HRV (RMSSD)",
                value = state.hrv?.let { "%.1f".format(it.rmssdMillis) },
                unit = "ms",
                subtitle = if (state.hrv != null) "Original record" else "No record today",
                icon = Icons.Default.Timeline,
                accent = HrvEmerald,
                onClick = { onOpenTab(VayuTab.VITALS) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HealthMetricCard(
                title = "Blood oxygen",
                value = state.respiratory?.spo2Percentage?.let { "%.1f".format(it) },
                unit = "%",
                subtitle = if (state.respiratory?.spo2Percentage != null) "Original record" else "No record today",
                icon = Icons.Default.Timeline,
                accent = Spo2Blue,
                onClick = { onOpenTab(VayuTab.VITALS) },
                modifier = Modifier.weight(1f)
            )
            HealthMetricCard(
                title = "Respiratory rate",
                value = state.respiratory?.breathsPerMinute?.let { "%.1f".format(it) },
                unit = "br/min",
                subtitle = if (state.respiratory?.breathsPerMinute != null) "Original record" else "No record today",
                icon = Icons.Default.Timeline,
                accent = RespirationTeal,
                onClick = { onOpenTab(VayuTab.VITALS) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = "Tap any card to explore its original Health Connect records. Vayu does not calculate readiness, strain, stress, or sleep scores.",
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(88.dp))
    }
}

@Composable
private fun HealthMetricCard(
    title: String,
    value: String?,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .height(164.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(CardBackgroundGlass.copy(alpha = 0.95f), Color(0xFF0B0E15))))
            .border(1.dp, Brush.linearGradient(listOf(accent.copy(alpha = 0.52f), GlassBorder)), shape)
            .clickable(onClick = onClick)
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.55.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(18.dp))
        if (value == null) {
            Text("—", fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = TextUnavailable)
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, letterSpacing = (-0.8).sp, maxLines = 1)
                if (unit != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.padding(bottom = 5.dp))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(subtitle, fontSize = 10.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

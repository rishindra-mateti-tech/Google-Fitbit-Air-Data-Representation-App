package com.vayu.health.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
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
import com.vayu.health.data.model.ProvenanceFormatter
import com.vayu.health.ui.components.GlassCard
import com.vayu.health.ui.components.GlassPill
import com.vayu.health.ui.components.MetricTile
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

@Composable
fun ActivityScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val activity = state.activity

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
                text = "Activity & Movement",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            val pillText = ProvenanceFormatter.formatCompact(state.selectedSource)
            GlassPill(
                text = pillText,
                dotColor = if (state.selectedSource != null) VayuCyan else EnergyAmber
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Steps Tile
        val stepsStr = activity?.steps?.let { "%,d".format(it) }
        val isFiltered = state.selectedSource != null
        val updatedTimeStr = if (isFiltered) {
            "Fitbit Health Connect total for today"
        } else {
            activity?.lastUpdated?.let { "Aggregated as of ${viewModel.formatTime(it)}" } ?: "Health Connect aggregated total"
        }
        val sourcesLabel = ProvenanceFormatter.formatCompact(state.selectedSource)

        MetricTile(
            title = "Total Steps",
            value = stepsStr,
            unit = if (stepsStr != null) "steps" else null,
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            accentColor = VayuCyan,
            timestamp = updatedTimeStr,
            source = sourcesLabel
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Total Energy Tile (Authentic Fitbit aggregate: e.g. 1790 kcal)
        val totalCalStr = activity?.totalCaloriesKcal?.let { "%.0f".format(it) }
        MetricTile(
            title = "Total Energy",
            value = totalCalStr,
            unit = if (totalCalStr != null) "kcal" else null,
            icon = Icons.Default.LocalFireDepartment,
            accentColor = EnergyAmber,
            timestamp = if (totalCalStr != null) "Fitbit daily energy burn" else null,
            source = sourcesLabel
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Active Energy Tile
        val activeCalStr = activity?.activeCaloriesKcal?.let { "%.0f".format(it) }
        MetricTile(
            title = "Active Energy",
            value = activeCalStr,
            unit = if (activeCalStr != null) "kcal" else null,
            icon = Icons.Default.LocalFireDepartment,
            accentColor = VayuBlue,
            timestamp = if (activeCalStr != null) "Fitbit active workout burn" else null,
            source = sourcesLabel
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Distance Tile
        val distKm = activity?.distanceMeters?.let { "%.2f".format(it / 1000.0) }
        MetricTile(
            title = "Distance",
            value = distKm,
            unit = if (distKm != null) "km" else null,
            icon = Icons.Default.Map,
            accentColor = RespirationTeal,
            source = sourcesLabel
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Activity Transparency
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
                    text = "Activity totals are queried via Health Connect's Aggregate API to prevent double-counting across connected fitness trackers. No synthetic strain scores are calculated.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

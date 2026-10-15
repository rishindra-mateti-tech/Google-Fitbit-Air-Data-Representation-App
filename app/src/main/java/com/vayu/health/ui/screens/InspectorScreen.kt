package com.vayu.health.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.HealthDashboardState
import com.vayu.health.data.model.ProvenanceFormatter
import com.vayu.health.ui.components.GlassCard
import com.vayu.health.ui.components.GlassPill
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

@Composable
fun InspectorScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val friendlySource = ProvenanceFormatter.formatDisplayName(state.selectedSource)
    val compactSource = ProvenanceFormatter.formatCompact(state.selectedSource)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Data & Provenance Inspector",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Source: $friendlySource",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = VayuCyan
                    )
                }

                IconButton(
                    onClick = { viewModel.loadMetrics(forceRefresh = true) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Health Connect Records",
                        tint = VayuCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. Section: Today's Health Connect Aggregates Used by Vayu
        item {
            Text(
                text = "TODAY'S HEALTH CONNECT AGGREGATES USED BY VAYU",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VayuCyan,
                letterSpacing = 0.5.sp
            )
        }

        item {
            val act = state.activity
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Explanatory Banner
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x1200F2FE))
                            .border(0.5.dp, VayuCyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = VayuCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Health Connect aggregates calculate daily sums directly from the device's wearable sync store. Values below are exact Health Connect measurements currently represented in your dashboard.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Aggregate Rows
                    AggregateInspectorRow(
                        metricName = "Total Energy",
                        value = act?.totalCaloriesKcal?.let { "%.0f kcal".format(it) } ?: "Data unavailable",
                        source = compactSource,
                        accentColor = EnergyAmber,
                        hasValue = act?.totalCaloriesKcal != null
                    )

                    HorizontalDivider(color = Color(0x14FFFFFF))

                    AggregateInspectorRow(
                        metricName = "Active Energy",
                        value = act?.activeCaloriesKcal?.let { "%.0f kcal".format(it) } ?: "Data unavailable",
                        source = compactSource,
                        accentColor = EnergyAmber,
                        hasValue = act?.activeCaloriesKcal != null
                    )

                    HorizontalDivider(color = Color(0x14FFFFFF))

                    AggregateInspectorRow(
                        metricName = "Daily Steps",
                        value = act?.steps?.let { "%,d steps".format(it) } ?: "Data unavailable",
                        source = compactSource,
                        accentColor = VayuCyan,
                        hasValue = act?.steps != null
                    )

                    HorizontalDivider(color = Color(0x14FFFFFF))

                    AggregateInspectorRow(
                        metricName = "Daily Distance",
                        value = act?.distanceMeters?.let { "%.2f km".format(it / 1000.0) } ?: "Data unavailable",
                        source = compactSource,
                        accentColor = RespirationTeal,
                        hasValue = act?.distanceMeters != null
                    )

                    HorizontalDivider(color = Color(0x14FFFFFF))

                    AggregateInspectorRow(
                        metricName = "Resting Heart Rate",
                        value = state.restingHeartRate?.let { "${it.beatsPerMinute} bpm" } ?: "Data unavailable",
                        source = compactSource,
                        accentColor = HeartCrimson,
                        hasValue = state.restingHeartRate != null
                    )

                    HorizontalDivider(color = Color(0x14FFFFFF))

                    AggregateInspectorRow(
                        metricName = "HRV (RMSSD)",
                        value = state.hrv?.let { "%.1f ms".format(it.rmssdMillis) } ?: "Data unavailable",
                        source = compactSource,
                        accentColor = HrvEmerald,
                        hasValue = state.hrv != null
                    )

                    HorizontalDivider(color = Color(0x14FFFFFF))

                    AggregateInspectorRow(
                        metricName = "Actual Sleep Duration",
                        value = state.sleep?.actualSleepDurationMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "Data unavailable",
                        source = compactSource,
                        accentColor = SleepRem,
                        hasValue = state.sleep?.actualSleepDurationMinutes != null
                    )
                }
            }
        }

        // 3. Section: Individual Raw Records in Time Window
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INDIVIDUAL RECORDS IN THIS TIME WINDOW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${state.rawRecords.size} records",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.rawRecords.isNotEmpty()) StatusVerified else TextMuted
                )
            }
        }

        if (state.rawRecords.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0x10FFFFFF)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = StatusWarning,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "0 Individual Records in Window",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (state.activity?.totalCaloriesKcal != null) {
                                "$compactSource has written aggregate totals (${state.activity.totalCaloriesKcal.toInt()} kcal Total Energy) to Health Connect, but has not written granular per-sample log entries for this 24h window."
                            } else {
                                "$compactSource has not written individual per-sample records to Health Connect for this 24h window."
                            },
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            items(state.rawRecords) { record ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    backgroundColor = Color(0x14FFFFFF)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = record.recordType,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VayuCyan
                            )
                            GlassPill(
                                text = ProvenanceFormatter.formatCompact(record.clientPackage),
                                textColor = TextSecondary,
                                backgroundColor = Color(0x0EFFFFFF)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = record.valueSummary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Start: ${viewModel.formatTime(record.startTime)}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            if (record.endTime != null) {
                                Text(
                                    text = "End: ${viewModel.formatTime(record.endTime)}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Origin: ${record.clientPackage}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AggregateInspectorRow(
    metricName: String,
    value: String,
    source: String,
    accentColor: Color,
    hasValue: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = metricName,
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Source: $source",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (hasValue) accentColor else TextUnavailable
        )
    }
}

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Selected Raw Health Connect Records",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                val filterLabel = state.selectedSource?.let { "Source: $it" } ?: "All Health Connect Sources"
                Text(
                    text = "$filterLabel • ${state.rawRecords.size} records",
                    fontSize = 12.sp,
                    color = VayuCyan
                )
            }

            IconButton(
                onClick = { viewModel.loadMetrics() },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1AFFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Health Connect Records",
                    tint = VayuCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostic summary card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (state.rawRecords.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.BugReport,
                        contentDescription = "Status",
                        tint = if (state.rawRecords.isNotEmpty()) StatusVerified else StatusWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.rawRecords.isNotEmpty()) "Connected to Health Connect" else "No Records Captured in Window",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Inspecting 24h window. Found ${state.rawRecords.size} record entries from Health Connect sources across all requested types.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (state.rawRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "No Raw Records in Window",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Confirm your wearable or fitness tracking app is synced with Google Health Connect.\n2. In Health Connect Settings > App permissions, verify Vayu has read permissions enabled.\n3. Tap refresh above to query updated records.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.rawRecords) { record ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
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
                                    text = record.clientPackage.substringAfterLast("."),
                                    textColor = TextMuted,
                                    backgroundColor = Color(0x0EFFFFFF)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = record.valueSummary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

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

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "ID: ${record.recordId.take(16)}...",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

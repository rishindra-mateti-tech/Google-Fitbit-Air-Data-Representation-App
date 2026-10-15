package com.vayu.health.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
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
import com.vayu.health.data.model.SleepStageType
import com.vayu.health.ui.components.GlassCard
import com.vayu.health.ui.components.GlassPill
import com.vayu.health.ui.components.MetricTile
import com.vayu.health.ui.components.SleepStageBar
import com.vayu.health.ui.theme.*
import com.vayu.health.ui.viewmodel.HealthDashboardViewModel

@Composable
fun SleepScreen(
    state: HealthDashboardState,
    viewModel: HealthDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val sleep = state.sleep

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Sleep Architecture",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            GlassPill(
                text = "Health Connect",
                dotColor = SleepRem
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Total Duration Tile
        val durationStr = sleep?.let { viewModel.formatDuration(it.totalDurationMinutes) }
        MetricTile(
            title = "Total Sleep Duration",
            value = durationStr,
            icon = Icons.Default.Bedtime,
            accentColor = SleepRem,
            timestamp = sleep?.let { "Recorded by ${it.sourcePackage}" },
            statusText = if (sleep != null) "Verified" else null
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (sleep != null) {
            // Sleep Timing Cards: Bedtime & Wake Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = "BEDTIME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = viewModel.formatTime(sleep.startTime),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                GlassCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = "WAKE TIME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = viewModel.formatTime(sleep.endTime),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stages Graph Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "STAGE BREAKDOWN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SleepStageBar(sleep = sleep)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stage List Segments
            if (sleep.stages.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "RECORDED STAGE SEGMENTS (${sleep.stages.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        sleep.stages.take(8).forEach { stage ->
                            val color = when (stage.stage) {
                                SleepStageType.DEEP -> SleepDeep
                                SleepStageType.REM -> SleepRem
                                SleepStageType.LIGHT -> SleepLight
                                SleepStageType.AWAKE -> SleepAwake
                                SleepStageType.UNKNOWN -> TextMuted
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stage.stage.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "${stage.durationMinutes} min (${viewModel.formatTime(stage.startTime)} - ${viewModel.formatTime(stage.endTime)})",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Data Transparency Notice
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
                    text = "Google's proprietary 0-100 Sleep Score is not exported to Health Connect. Vayu never fabricates a replacement score, presenting strictly your recorded stages and duration.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

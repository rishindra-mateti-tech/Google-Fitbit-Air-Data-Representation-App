package com.vayu.health.ui.screens

import androidx.compose.foundation.background
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
import com.vayu.health.ui.components.SleepOutOfBed
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
                text = sleep?.sourcePackage ?: "Health Connect",
                dotColor = SleepRem
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Actual Sleep Duration Tile
        val actualSleepStr = sleep?.actualSleepDurationMinutes?.let { viewModel.formatDuration(it) }
        val sessionElapsedStr = sleep?.sessionElapsedMinutes?.let { viewModel.formatDuration(it) }

        MetricTile(
            title = "Actual Sleep Duration",
            value = actualSleepStr,
            icon = Icons.Default.Bedtime,
            accentColor = SleepRem,
            timestamp = sleep?.let { "Recorded by ${it.sourcePackage}" },
            source = sleep?.sourcePackage ?: (state.selectedSource ?: "Health Connect")
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (sessionElapsedStr != null) {
            MetricTile(
                title = "Session Elapsed Time",
                value = sessionElapsedStr,
                icon = Icons.Default.Info,
                accentColor = SleepDeep,
                timestamp = "In-bed to out-of-bed interval",
                source = sleep.sourcePackage
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (sleep != null) {
            // Timing Cards: Bedtime & Wake Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = "SESSION START",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = viewModel.formatTime(sleep.startTime),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                GlassCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = "SESSION END",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = viewModel.formatTime(sleep.endTime),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            if (actualSleepStr != null) {
                Spacer(modifier = Modifier.height(14.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("TOTAL SESSION TIME", fontSize = 11.sp, color = TextMuted)
                            Text("${sleep.sessionElapsedMinutes / 60}h ${sleep.sessionElapsedMinutes % 60}m", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("AWAKE / OUT OF BED", fontSize = 11.sp, color = TextMuted)
                            Text("${sleep.awakeMinutes + sleep.outOfBedMinutes} min", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SleepAwake)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stages Graph Card
            if (sleep.stages.isNotEmpty()) {
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

                        sleep.stages.forEach { stage ->
                            val (color, name) = when (stage.stage) {
                                SleepStageType.DEEP -> SleepDeep to "Deep Sleep"
                                SleepStageType.REM -> SleepRem to "REM Sleep"
                                SleepStageType.LIGHT -> SleepLight to "Light Sleep"
                                SleepStageType.SLEEPING -> Color(0xFF4FC3F7) to "Sleeping (Generic)"
                                SleepStageType.AWAKE -> SleepAwake to "Awake"
                                SleepStageType.OUT_OF_BED -> SleepOutOfBed to "Out of Bed"
                                SleepStageType.UNKNOWN -> TextMuted to "Unknown"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
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
                                        text = name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "${stage.durationMinutes} min (${viewModel.formatTime(stage.startTime)} - ${viewModel.formatTime(stage.endTime)})",
                                    fontSize = 11.sp,
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
                    text = "Vayu visualizes only raw SleepSessionRecord intervals and stages from Health Connect. Proprietary sleep scores and calculated metrics are excluded.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

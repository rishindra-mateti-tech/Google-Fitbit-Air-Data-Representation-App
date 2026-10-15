package com.vayu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.SleepMetric
import com.vayu.health.data.model.SleepStageType
import com.vayu.health.ui.theme.*

val SleepOutOfBed = Color(0xFFFF7043)

@Composable
fun SleepStageBar(
    sleep: SleepMetric,
    modifier: Modifier = Modifier
) {
    val total = (sleep.deepMinutes + sleep.remMinutes + sleep.lightMinutes +
            sleep.sleepingMinutes + sleep.awakeMinutes + sleep.outOfBedMinutes).coerceAtLeast(1L)

    val deepWeight = (sleep.deepMinutes.toFloat() / total).coerceAtLeast(0f)
    val remWeight = (sleep.remMinutes.toFloat() / total).coerceAtLeast(0f)
    val lightWeight = (sleep.lightMinutes.toFloat() / total).coerceAtLeast(0f)
    val sleepingWeight = (sleep.sleepingMinutes.toFloat() / total).coerceAtLeast(0f)
    val awakeWeight = (sleep.awakeMinutes.toFloat() / total).coerceAtLeast(0f)
    val outOfBedWeight = (sleep.outOfBedMinutes.toFloat() / total).coerceAtLeast(0f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Multi-segment horizontal bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0x15FFFFFF))
        ) {
            if (deepWeight > 0) {
                Box(modifier = Modifier.fillMaxHeight().weight(deepWeight).background(SleepDeep))
            }
            if (remWeight > 0) {
                Box(modifier = Modifier.fillMaxHeight().weight(remWeight).background(SleepRem))
            }
            if (lightWeight > 0) {
                Box(modifier = Modifier.fillMaxHeight().weight(lightWeight).background(SleepLight))
            }
            if (sleepingWeight > 0) {
                Box(modifier = Modifier.fillMaxHeight().weight(sleepingWeight).background(Color(0xFF4FC3F7)))
            }
            if (awakeWeight > 0) {
                Box(modifier = Modifier.fillMaxHeight().weight(awakeWeight).background(SleepAwake))
            }
            if (outOfBedWeight > 0) {
                Box(modifier = Modifier.fillMaxHeight().weight(outOfBedWeight).background(SleepOutOfBed))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend with exact durations for each stage
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (sleep.deepMinutes > 0) {
                    StageLegendItem(label = "Deep", minutes = sleep.deepMinutes, color = SleepDeep)
                }
                if (sleep.remMinutes > 0) {
                    StageLegendItem(label = "REM", minutes = sleep.remMinutes, color = SleepRem)
                }
                if (sleep.lightMinutes > 0) {
                    StageLegendItem(label = "Light", minutes = sleep.lightMinutes, color = SleepLight)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (sleep.sleepingMinutes > 0) {
                    StageLegendItem(label = "Sleeping (Unspecified)", minutes = sleep.sleepingMinutes, color = Color(0xFF4FC3F7))
                }
                if (sleep.awakeMinutes > 0) {
                    StageLegendItem(label = "Awake", minutes = sleep.awakeMinutes, color = SleepAwake)
                }
                if (sleep.outOfBedMinutes > 0) {
                    StageLegendItem(label = "Out of Bed", minutes = sleep.outOfBedMinutes, color = SleepOutOfBed)
                }
            }
        }
    }
}

@Composable
private fun StageLegendItem(
    label: String,
    minutes: Long,
    color: Color
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
        val h = minutes / 60
        val m = minutes % 60
        val text = if (h > 0) "${h}h ${m}m" else "${m}m"
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(start = 14.dp, top = 2.dp)
        )
    }
}

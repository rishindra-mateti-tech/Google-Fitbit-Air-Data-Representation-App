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
import com.vayu.health.ui.theme.*

@Composable
fun SleepStageBar(
    sleep: SleepMetric,
    modifier: Modifier = Modifier
) {
    val total = (sleep.deepMinutes + sleep.remMinutes + sleep.lightMinutes + sleep.awakeMinutes).coerceAtLeast(1L)
    val deepWeight = (sleep.deepMinutes.toFloat() / total).coerceAtLeast(0.01f)
    val remWeight = (sleep.remMinutes.toFloat() / total).coerceAtLeast(0.01f)
    val lightWeight = (sleep.lightMinutes.toFloat() / total).coerceAtLeast(0.01f)
    val awakeWeight = (sleep.awakeMinutes.toFloat() / total).coerceAtLeast(0.01f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Multi-segment horizontal bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0x15FFFFFF))
        ) {
            if (sleep.deepMinutes > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(deepWeight)
                        .background(SleepDeep)
                )
            }
            if (sleep.remMinutes > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(remWeight)
                        .background(SleepRem)
                )
            }
            if (sleep.lightMinutes > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(lightWeight)
                        .background(SleepLight)
                )
            }
            if (sleep.awakeMinutes > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(awakeWeight)
                        .background(SleepAwake)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legend with exact durations
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StageLegendItem(label = "Deep", minutes = sleep.deepMinutes, color = SleepDeep)
            StageLegendItem(label = "REM", minutes = sleep.remMinutes, color = SleepRem)
            StageLegendItem(label = "Light", minutes = sleep.lightMinutes, color = SleepLight)
            StageLegendItem(label = "Awake", minutes = sleep.awakeMinutes, color = SleepAwake)
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

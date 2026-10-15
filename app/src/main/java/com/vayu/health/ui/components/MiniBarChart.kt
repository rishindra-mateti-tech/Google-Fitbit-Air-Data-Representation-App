package com.vayu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.ui.theme.TextMuted
import com.vayu.health.ui.theme.TextSecondary

data class BarDataPoint(
    val label: String,
    val value: Float, // 0.0f to 1.0f or raw
    val formattedValue: String? = null,
    val isHighlighted: Boolean = false
)

@Composable
fun MiniBarChart(
    points: List<BarDataPoint>,
    primaryColor: Color,
    secondaryColor: Color = primaryColor.copy(alpha = 0.35f),
    modifier: Modifier = Modifier,
    chartHeight: Dp = 100.dp,
    barWidth: Dp = 12.dp
) {
    val maxValue = points.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEach { point ->
                val ratio = (point.value / maxValue).coerceIn(0.08f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Bar pill
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .fillMaxHeight(ratio)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(
                                if (point.isHighlighted) {
                                    Brush.verticalGradient(
                                        listOf(
                                            primaryColor,
                                            primaryColor.copy(alpha = 0.6f)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(
                                            secondaryColor,
                                            secondaryColor.copy(alpha = 0.2f)
                                        )
                                    )
                                }
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Label
                    Text(
                        text = point.label,
                        fontSize = 10.sp,
                        fontWeight = if (point.isHighlighted) FontWeight.Bold else FontWeight.Medium,
                        color = if (point.isHighlighted) primaryColor else TextMuted
                    )
                }
            }
        }
    }
}

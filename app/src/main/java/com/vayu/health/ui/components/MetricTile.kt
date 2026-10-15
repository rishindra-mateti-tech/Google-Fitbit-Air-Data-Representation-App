package com.vayu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.ui.theme.*

@Composable
fun MetricTile(
    title: String,
    value: String?,
    unit: String? = null,
    icon: ImageVector,
    accentColor: Color,
    timestamp: String? = null,
    source: String,
    modifier: Modifier = Modifier,
    statusText: String? = null
) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CardBackgroundGlassElevated,
                        CardBackgroundGlass
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.45f),
                        GlassBorder,
                        Color(0x05FFFFFF)
                    )
                ),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header: Icon + Title + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(0.5.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                }

                if (statusText != null) {
                    GlassPill(
                        text = statusText,
                        textColor = accentColor,
                        backgroundColor = accentColor.copy(alpha = 0.12f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body: Large Measurement Value or "Data unavailable"
            if (value != null) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = value,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    if (unit != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = unit,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            } else {
                Column {
                    Text(
                        text = "Data unavailable",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextUnavailable
                    )
                    Text(
                        text = "Not reported to Health Connect",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Timestamp & Source Transparency
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (value != null && timestamp != null) timestamp else "Original Health Record",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = source,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }
    }
}

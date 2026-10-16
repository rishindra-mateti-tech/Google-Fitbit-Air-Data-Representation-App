package com.vayu.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.ui.theme.*

enum class RangeStatus(val label: String, val color: Color) {
    TYPICAL("Typical", BaroReadinessTeal),
    HIGHER("Higher", BaroStrainBlue),
    LOWER("Lower", BaroHeartCoral),
    BUILDING("Building baseline", BaroTextMuted),
    UNAVAILABLE("No record", BaroTextMuted)
}

/**
 * Baro vertical range pill gauge.
 * Displays a vertical capsule showing the user's 30-night baseline range
 * and today's reading positioned relative to it.
 */
@Composable
fun BaroVerticalRangePill(
    currentValueFraction: Float?, // 0.0 (bottom) to 1.0 (top), or null if building/unavailable
    status: RangeStatus,
    accentColor: Color,
    modifier: Modifier = Modifier,
    rangeStartFraction: Float = 0.35f, // typical range lower bound
    rangeEndFraction: Float = 0.65f,   // typical range upper bound
    pillWidth: Dp = 6.dp,
    pillHeight: Dp = 32.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Vertical Capsule
        Canvas(
            modifier = Modifier
                .width(pillWidth)
                .height(pillHeight)
        ) {
            val w = size.width
            val h = size.height
            val corner = CornerRadius(w / 2f, w / 2f)

            // Background Track
            drawRoundRect(
                color = BaroPillTrack,
                size = Size(w, h),
                cornerRadius = corner
            )

            if (currentValueFraction != null) {
                // Typical Range Shaded Band
                val bandTop = h * (1f - rangeEndFraction)
                val bandBottom = h * (1f - rangeStartFraction)
                val bandH = (bandBottom - bandTop).coerceAtLeast(4.dp.toPx())

                drawRoundRect(
                    color = BaroPillRange,
                    topLeft = Offset(0f, bandTop),
                    size = Size(w, bandH),
                    cornerRadius = CornerRadius(w / 2f, w / 2f)
                )

                // Today's Reading Indicator Dot
                val dotY = (h * (1f - currentValueFraction.coerceIn(0.05f, 0.95f)))
                drawCircle(
                    color = accentColor,
                    radius = w * 0.9f,
                    center = Offset(w / 2f, dotY)
                )
            }
        }

        // Status Text
        val prefix = when (status) {
            RangeStatus.TYPICAL -> "✓ "
            RangeStatus.HIGHER -> "^ "
            RangeStatus.LOWER -> "v "
            else -> ""
        }
        Text(
            text = "$prefix${status.label}",
            fontSize = 11.sp,
            color = status.color,
            maxLines = 1
        )
    }
}

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.ui.theme.*

enum class RangeStatus(val label: String, val color: Color) {
    TYPICAL("Typical", Color(0xFF00D4AA)),      // Baro Teal/Green
    HIGHER("Higher", Color(0xFFFFA726)),        // Baro Orange
    LOWER("Lower", Color(0xFF4DA6FF)),          // Baro Blue
    BUILDING("Building baseline", Color(0xFF71717A)),
    UNAVAILABLE("No readings yet", Color(0xFF71717A))
}

/**
 * Baro vertical range capsule pill matching Screenshot 5.
 * Features:
 * - Vertical capsule (track: #242428)
 * - Shaded typical baseline range band in middle (#40404A)
 * - Colored hollow circular marker at today's value relative to typical range
 * - Subtle dark empty track when building baseline
 */
@Composable
fun BaroVerticalRangePill(
    status: RangeStatus,
    modifier: Modifier = Modifier,
    pillWidth: Dp = 8.dp,
    pillHeight: Dp = 40.dp
) {
    Canvas(
        modifier = modifier
            .width(pillWidth)
            .height(pillHeight)
    ) {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(w / 2f, w / 2f)

        // Background Track
        drawRoundRect(
            color = Color(0xFF222226),
            size = Size(w, h),
            cornerRadius = corner
        )

        // If in calibrated status (TYPICAL, HIGHER, LOWER)
        if (status == RangeStatus.TYPICAL || status == RangeStatus.HIGHER || status == RangeStatus.LOWER) {
            // Shaded Typical Range Band (middle 35% to 65% of capsule)
            val bandTop = h * 0.35f
            val bandHeight = h * 0.30f
            drawRoundRect(
                color = Color(0xFF40404A),
                topLeft = Offset(0f, bandTop),
                size = Size(w, bandHeight),
                cornerRadius = CornerRadius(w / 2f, w / 2f)
            )

            // Center indicator line
            drawLine(
                color = Color(0x30FFFFFF),
                start = Offset(w / 2f, 4.dp.toPx()),
                end = Offset(w / 2f, h - 4.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )

            // Marker position based on status
            val markerY = when (status) {
                RangeStatus.HIGHER -> h * 0.18f // Above the typical band
                RangeStatus.TYPICAL -> h * 0.50f // Inside the typical band
                RangeStatus.LOWER -> h * 0.82f  // Below the typical band
                else -> h * 0.50f
            }

            // Draw outer dark mask
            drawCircle(
                color = Color(0xFF121214),
                radius = w * 0.85f,
                center = Offset(w / 2f, markerY)
            )

            // Draw colored indicator ring (as seen in Screenshot 5)
            drawCircle(
                color = status.color,
                radius = w * 0.80f,
                center = Offset(w / 2f, markerY),
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw inner dot
            drawCircle(
                color = status.color,
                radius = w * 0.30f,
                center = Offset(w / 2f, markerY)
            )
        }
    }
}

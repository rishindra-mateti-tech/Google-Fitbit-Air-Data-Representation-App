package com.vayu.health.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.ui.theme.TextMuted
import com.vayu.health.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Baro & Vora inspired circular ring dial with glowing sweep gradient and rounded cap.
 */
@Composable
fun CircularProgressRing(
    progress: Float, // 0.0f to 1.0f (can exceed 1.0f)
    ringColor: Color,
    gradientEndColor: Color = ringColor,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = Color(0x18FFFFFF),
    centerValue: String? = null,
    centerUnit: String? = null,
    centerLabel: String? = null
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension
            val radius = diameter / 2f
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active Sweep Arc
            if (clampedProgress > 0.001f) {
                val sweepAngle = clampedProgress * 360f
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to ringColor,
                        clampedProgress to gradientEndColor,
                        center = Offset(size.width / 2f, size.height / 2f)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Glowing Head Indicator Dot
                val endAngleRad = Math.toRadians((-90f + sweepAngle).toDouble())
                val headX = (size.width / 2f) + radius * cos(endAngleRad).toFloat()
                val headY = (size.height / 2f) + radius * sin(endAngleRad).toFloat()

                drawCircle(
                    color = gradientEndColor,
                    radius = strokePx / 1.7f,
                    center = Offset(headX, headY)
                )
                drawCircle(
                    color = Color.White,
                    radius = strokePx / 3.5f,
                    center = Offset(headX, headY)
                )
            }
        }

        // Center Content
        if (centerValue != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (centerLabel != null) {
                    Text(
                        text = centerLabel.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = centerValue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (centerUnit != null) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = centerUnit,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

package com.vayu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.vayu.health.ui.theme.GlassBorder
import com.vayu.health.ui.theme.TextPrimary

@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    textColor: Color = TextPrimary,
    backgroundColor: Color = Color(0x1AFFFFFF)
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .border(0.75.dp, GlassBorder, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

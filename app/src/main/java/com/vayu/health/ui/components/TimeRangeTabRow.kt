package com.vayu.health.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.health.data.model.TimeRangeTab
import com.vayu.health.ui.theme.BaroBlack
import com.vayu.health.ui.theme.BaroBorder
import com.vayu.health.ui.theme.BaroTextMuted
import com.vayu.health.ui.theme.BaroTextWhite

/**
 * Pixel-perfect Google Health time-range tab selector: [ D | W | M | 3M | Y ]
 * Elevated obsidian pill container with smooth transition animations.
 */
@Composable
fun TimeRangeTabRow(
    selectedTab: TimeRangeTab,
    onTabSelected: (TimeRangeTab) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF00D4AA)
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141418))
            .border(1.dp, BaroBorder, RoundedCornerShape(20.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeRangeTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF22222A) else Color.Transparent,
                animationSpec = tween(durationMillis = 180),
                label = "tab_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) activeColor else BaroTextMuted,
                animationSpec = tween(durationMillis = 180),
                label = "tab_text"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

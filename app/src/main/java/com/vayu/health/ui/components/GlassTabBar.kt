package com.vayu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
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

enum class VayuTab(val title: String, val icon: ImageVector) {
    OVERVIEW("Vayu", Icons.Default.Air),
    SLEEP("Sleep", Icons.Default.Bedtime),
    VITALS("Vitals", Icons.Default.Favorite),
    ACTIVITY("Activity", Icons.Default.DirectionsRun),
    INSPECTOR("Data", Icons.Default.Search)
}

@Composable
fun GlassTabBar(
    selectedTab: VayuTab,
    onTabSelected: (VayuTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(shape)
            .background(Color(0xE60A0E17))
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        GlassBorderHighlight,
                        GlassBorder,
                        Color(0x05FFFFFF)
                    )
                ),
                shape
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VayuTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val tint = if (isSelected) VayuCyan else TextMuted

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = tint
                    )
                }
            }
        }
    }
}

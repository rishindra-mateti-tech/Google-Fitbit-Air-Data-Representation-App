package com.vayu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vayu.health.ui.theme.CardBackgroundGlass
import com.vayu.health.ui.theme.GlassBorder
import com.vayu.health.ui.theme.GlassBorderHighlight

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    borderBrush: Brush = Brush.linearGradient(
        colors = listOf(
            GlassBorderHighlight,
            GlassBorder,
            Color(0x08FFFFFF)
        )
    ),
    backgroundColor: Color = CardBackgroundGlass,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = shape
            )
            .padding(18.dp),
        content = content
    )
}

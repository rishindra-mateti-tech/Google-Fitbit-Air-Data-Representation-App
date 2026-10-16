package com.vayu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vayu.health.ui.theme.BaroBorder
import com.vayu.health.ui.theme.BaroCard

/**
 * Standard Baro surface card container with rounded corners (20.dp),
 * dark surface (#121214), and subtle border (#242428).
 */
@Composable
fun BaroCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BaroCard,
    borderColor: Color = BaroBorder,
    cornerRadius: Dp = 20.dp,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .then(clickableModifier)
            .padding(padding),
        content = content
    )
}

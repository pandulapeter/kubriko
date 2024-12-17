package com.pandulapeter.kubriko.gameWallbreaker.implementation.ui

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal fun WallbreakerTheme(
    content: @Composable () -> Unit,
) = MaterialTheme(
    colorScheme = darkColorScheme(
        primaryContainer = Color.LightGray,
        primary = Color.LightGray,
        onPrimary = Color.Black,
    ),
    shapes = Shapes(
        extraSmall = Shape,
        small = Shape,
        medium = Shape,
        large = Shape,
        extraLarge = Shape,
    ),
    content = content
)

private val Shape: CornerBasedShape = RoundedCornerShape(
    topStart = CornerSize(0),
    topEnd = CornerSize(0),
    bottomStart = CornerSize(0),
    bottomEnd = CornerSize(0),
)

package com.pandulapeter.kubriko.gameWallbreaker.implementation.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun WallbreakerCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = Card(
    modifier = modifier.border(
        width = 1.dp,
        color = Color.Gray,
    ),
    colors = CardDefaults.cardColors().copy(
        containerColor = Color.Black,
    ),
    content = content,
)
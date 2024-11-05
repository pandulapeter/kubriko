package com.pandulapeter.kubriko.debugInfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.debugInfo.implementation.DebugInfoHelper
import kotlin.math.roundToInt

/**
 * TODO: Documentation
 */
// TODO: Make this Composable configurable
@Composable
fun DebugInfo(
    modifier: Modifier = Modifier,
    kubriko: Kubriko,
) {
    val debugInfoHelper = remember { DebugInfoHelper(kubriko) }
    val debugInfoMetadata = debugInfoHelper.debugInfoMetadata.collectAsState().value
    Text(
        modifier = modifier
            .defaultMinSize(minWidth = 200.dp)
            .background(Color.White)
            .padding(8.dp),
        style = TextStyle.Default.copy(fontSize = 10.sp),
        text = "FPS: ${debugInfoMetadata.fps.roundToInt()}\n" +
                "Total Actors: ${debugInfoMetadata.totalActorCount}\n" +
                "Visible within viewport: ${debugInfoMetadata.visibleActorWithinViewportCount}\n" +
                "Play time in seconds: ${debugInfoMetadata.playTimeInSeconds}"
    )
}
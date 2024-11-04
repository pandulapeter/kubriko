package com.pandulapeter.kubriko.debugInfo

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.debugInfo.implementation.DebugInfoManager
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
    val debugInfoManager = remember { DebugInfoManager(kubriko) }
    val debugInfoMetadata = debugInfoManager.debugInfoMetadata.collectAsState().value
    Text(
        modifier = modifier,
        text = "FPS: ${debugInfoMetadata.fps.roundToInt()}\n" +
                "Total Actors: ${debugInfoMetadata.totalActorCount}\n" +
                "Visible Actors within viewport: ${debugInfoMetadata.visibleActorWithinViewportCount}\n" +
                "Play time in seconds: ${debugInfoMetadata.playTimeInSeconds}"
    )
}
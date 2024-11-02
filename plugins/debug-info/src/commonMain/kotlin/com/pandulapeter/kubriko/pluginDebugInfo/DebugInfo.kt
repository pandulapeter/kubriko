package com.pandulapeter.kubriko.pluginDebugInfo

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.engine.Kubriko
import com.pandulapeter.kubriko.pluginDebugInfo.implementation.DebugInfoManager

/**
 * TODO: Documentation
 */
@Composable
fun DebugInfo(
    modifier: Modifier = Modifier,
    kubriko: Kubriko,
) {
    val debugInfoManager = remember { DebugInfoManager(kubriko) }
    val debugInfoMetadata = debugInfoManager.debugInfoMetadata.collectAsState().value
    Text(
        modifier = modifier,
        text = "FPS: ${debugInfoMetadata.fps.toString().subSequence(0, debugInfoMetadata.fps.toString().indexOf('.'))}\n" +
                "Total Actors: ${debugInfoMetadata.totalActorCount}\n" +
                "Visible Actors within viewport: ${debugInfoMetadata.visibleActorWithinViewportCount}\n" +
                "Play time in seconds: ${debugInfoMetadata.playTimeInSeconds}"
    )
}
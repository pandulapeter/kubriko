package com.pandulapeter.kubriko.debugMenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuController
import kotlin.math.roundToInt

/**
 * TODO: Documentation
 */
// TODO: Make this Composable configurable
// TODO: Should adapt to screen aspect ratio
@Composable
fun DebugMenu(
    modifier: Modifier = Modifier,
    kubriko: Kubriko,
    gameCanvas: @Composable BoxScope.() -> Unit,
) {
    val debugMenuController = remember { DebugMenuController(kubriko) }
    val debugInfoMetadata = debugMenuController.debugMenuMetadata.collectAsState().value
    val isDebugMenuVisible = remember { mutableStateOf(false) }
    Box(
        modifier = modifier,
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier.weight(1f),
            ) {
                gameCanvas()
            }
            AnimatedVisibility(
                visible = isDebugMenuVisible.value,
            ) {
                Text(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 200.dp)
                        .padding(8.dp),
                    style = TextStyle.Default.copy(fontSize = 10.sp),
                    text = "FPS: ${debugInfoMetadata.fps.roundToInt()}\n" +
                            "Total Actors: ${debugInfoMetadata.totalActorCount}\n" +
                            "Visible within viewport: ${debugInfoMetadata.visibleActorWithinViewportCount}\n" +
                            "Play time in seconds: ${debugInfoMetadata.playTimeInSeconds}"
                )
            }
        }
        Button(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            onClick = { isDebugMenuVisible.value = !isDebugMenuVisible.value }
        ) {
            Text(text = "Debug Menu")
        }
    }
}
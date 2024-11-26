package com.pandulapeter.kubriko.debugMenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuManager
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuMetadata
import kubriko.tools.debug_menu.generated.resources.Res
import kubriko.tools.debug_menu.generated.resources.debug_menu
import kubriko.tools.debug_menu.generated.resources.ic_debug
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * TODO: Documentation
 */
// TODO: Make this Composable configurable
// TODO: Should adapt to screen aspect ratio
@Composable
fun DebugMenu(
    modifier: Modifier = Modifier,
    debugMenuModifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    kubriko: Kubriko,
    gameCanvas: @Composable BoxScope.() -> Unit,
) {
    val debugMenuManager = remember { DebugMenuManager(kubriko) }
    val debugInfoMetadata = debugMenuManager.debugMenuMetadata.collectAsState(DebugMenuMetadata()).value
    val isDebugMenuVisible = remember { mutableStateOf(false) }
    Box(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier.weight(1f),
            ) {
                gameCanvas()
                KubrikoViewport(
                    kubriko = Kubriko.newInstance(debugMenuManager),
                )
            }
            AnimatedVisibility(
                visible = isDebugMenuVisible.value,
            ) {
                Surface(
                    modifier = Modifier.defaultMinSize(minWidth = 180.dp).fillMaxHeight(),
                    tonalElevation = when (isSystemInDarkTheme()) {
                        true -> 4.dp
                        false -> 0.dp
                    },
                    shadowElevation = when (isSystemInDarkTheme()) {
                        true -> 4.dp
                        false -> 2.dp
                    },
                ) {
                    Text(
                        modifier = debugMenuModifier.padding(16.dp),
                        style = TextStyle.Default.copy(fontSize = 10.sp),
                        text = "FPS: ${debugInfoMetadata.fps.roundToInt()}\n" +
                                "Total Actors: ${debugInfoMetadata.totalActorCount}\n" +
                                "Visible within viewport: ${debugInfoMetadata.visibleActorWithinViewportCount}\n" +
                                "Play time in seconds: ${debugInfoMetadata.playTimeInSeconds}"
                    )
                }
            }
        }
        Box(
            modifier = contentModifier.fillMaxSize().padding(16.dp),
        ) {
            FloatingActionButton(
                modifier = Modifier.size(40.dp).align(Alignment.BottomEnd),
                containerColor = MaterialTheme.colorScheme.primary,
                onClick = { isDebugMenuVisible.value = !isDebugMenuVisible.value },
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_debug),
                    contentDescription = stringResource(Res.string.debug_menu)
                )
            }
        }
    }
}
package com.pandulapeter.kubriko.debugMenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuManager
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuMetadata
import com.pandulapeter.kubriko.debugMenu.implementation.ui.DebugMenuContents
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.manager.ViewportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kubriko.tools.debug_menu.generated.resources.Res
import kubriko.tools.debug_menu.generated.resources.debug_menu
import kubriko.tools.debug_menu.generated.resources.ic_debug
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

object DebugMenu {

    private val _isDebugMenuVisible = mutableStateOf(false)
    internal val isDebugMenuVisible = _isDebugMenuVisible as State<Boolean>
    private val _isDebugOverlayEnabled = MutableStateFlow(false)
    internal val isDebugOverlayEnabled = _isDebugOverlayEnabled.asStateFlow()
    private val _logs = mutableStateOf(emptyList<String>())
    internal val logs = _logs as State<List<String>>

    fun log(message: String) {
        _logs.value = logs.value + message
    }

    internal fun onIsDebugMenuVisibleChanged() {
        _isDebugMenuVisible.value = !isDebugMenuVisible.value
    }

    internal fun onIsDebugOverlayEnabledChanged() = _isDebugOverlayEnabled.update { !it }
}

/**
 * TODO: Documentation
 */
// TODO: Make this Composable configurable)
@Composable
fun DebugMenu(
    modifier: Modifier = Modifier,
    debugMenuModifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    kubriko: Kubriko,
    isEnabled: Boolean = true,
    gameCanvas: @Composable BoxScope.() -> Unit,
) = Box(
    modifier = modifier,
) {
    // TODO: Should be persisted with Kubriko
    val debugMenuManager = remember { DebugMenuManager(kubriko) }
    val debugMenuKubriko = remember {
        Kubriko.newInstance(
            kubriko.get<ViewportManager>(),
            debugMenuManager,
        )
    }
    if (isEnabled) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier.weight(1f),
            ) {
                gameCanvas()
                KubrikoViewport(
                    kubriko = debugMenuKubriko,
                )
            }
            AnimatedVisibility(
                visible = DebugMenu.isDebugMenuVisible.value,
            ) {
                Surface(
                    modifier = Modifier
                        .defaultMinSize(
                            minWidth = 180.dp + WindowInsets.safeDrawing.only(WindowInsetsSides.Right).asPaddingValues()
                                .calculateRightPadding(LocalLayoutDirection.current)
                        ).fillMaxHeight(),
                    tonalElevation = when (isSystemInDarkTheme()) {
                        true -> 4.dp
                        false -> 0.dp
                    },
                    shadowElevation = when (isSystemInDarkTheme()) {
                        true -> 4.dp
                        false -> 2.dp
                    },
                ) {
                    DebugMenuContents(
                        modifier = debugMenuModifier,
                        debugMenuMetadata = debugMenuManager.debugMenuMetadata.collectAsState(DebugMenuMetadata()).value,
                        onIsDebugOverlayEnabledChanged = DebugMenu::onIsDebugOverlayEnabledChanged,
                    )
                }
            }
        }
        Box(
            modifier = contentModifier.fillMaxSize().padding(16.dp),
        ) {
            FloatingActionButton(
                modifier = Modifier.size(40.dp).align(Alignment.TopStart),
                containerColor = if (isSystemInDarkTheme()) {
                    if (DebugMenu.isDebugMenuVisible.value) MaterialTheme.colorScheme.primary else FloatingActionButtonDefaults.containerColor
                } else {
                    if (DebugMenu.isDebugMenuVisible.value) FloatingActionButtonDefaults.containerColor else MaterialTheme.colorScheme.primary
                },
                onClick = DebugMenu::onIsDebugMenuVisibleChanged,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_debug),
                    contentDescription = stringResource(Res.string.debug_menu)
                )
            }
        }
    } else {
        gameCanvas()
    }
}
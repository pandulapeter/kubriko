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
import androidx.compose.runtime.collectAsState
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
import com.pandulapeter.kubriko.logger.Logger
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kubriko.tools.debug_menu.generated.resources.Res
import kubriko.tools.debug_menu.generated.resources.debug_menu
import kubriko.tools.debug_menu.generated.resources.ic_debug_off
import kubriko.tools.debug_menu.generated.resources.ic_debug_on
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

object DebugMenu {

    private val _isVisible = MutableStateFlow(false)
    val isVisible = _isVisible.asStateFlow()
    private val _isDebugOverlayEnabled = MutableStateFlow(false)
    internal val isDebugOverlayEnabled = _isDebugOverlayEnabled.asStateFlow()
    private val _isLowPriorityEnabled = MutableStateFlow(true)
    internal val isLowPriorityEnabled = _isLowPriorityEnabled.asStateFlow()
    private val _isMediumPriorityEnabled = MutableStateFlow(true)
    internal val isMediumPriorityEnabled = _isMediumPriorityEnabled.asStateFlow()
    private val _isHighPriorityEnabled = MutableStateFlow(true)
    internal val isHighPriorityEnabled = _isHighPriorityEnabled.asStateFlow()
    internal val logs = combine(
        Logger.logs,
        isLowPriorityEnabled,
        isMediumPriorityEnabled,
        isHighPriorityEnabled,
    ) { logs,
        isLowPriorityEnabled,
        isMediumPriorityEnabled,
        isHighPriorityEnabled ->
        logs.filter {
            when (it.importance) {
                Logger.Importance.LOW -> isLowPriorityEnabled
                Logger.Importance.MEDIUM -> isMediumPriorityEnabled
                Logger.Importance.HIGH -> isHighPriorityEnabled
            }
        }
    }

    fun log(
        message: String,
        source: String? = null,
    ) = Logger.log(
        message = message,
        source = source,
    )

    fun clearLogs() = Logger.clearLogs()

    fun toggleVisibility() {
        _isVisible.value = !isVisible.value
    }

    internal fun onIsDebugOverlayEnabledChanged() = _isDebugOverlayEnabled.update { !it }

    internal fun onLowPriorityToggled() = _isLowPriorityEnabled.update { !it }

    internal fun onMediumPriorityToggled() = _isMediumPriorityEnabled.update { !it }

    internal fun onHighPriorityToggled() = _isHighPriorityEnabled.update { !it }
}

/**
 * TODO: Documentation
 */
@Composable
fun DebugMenu(
    modifier: Modifier = Modifier,
    debugMenuModifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    kubriko: Kubriko,
    isEnabled: Boolean = true,
    buttonAlignment: Alignment? = Alignment.TopEnd,
    theme: @Composable (@Composable () -> Unit) -> Unit = { KubrikoTheme(it) },
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
        val isVisible = DebugMenu.isVisible.collectAsState().value
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
                if (buttonAlignment != null) {
                    Box(
                        modifier = contentModifier.fillMaxSize().padding(16.dp),
                    ) {
                        FloatingActionButton(
                            modifier = Modifier.size(40.dp).align(buttonAlignment),
                            containerColor = if (isSystemInDarkTheme()) {
                                if (isVisible) MaterialTheme.colorScheme.primary else FloatingActionButtonDefaults.containerColor
                            } else {
                                if (isVisible) FloatingActionButtonDefaults.containerColor else MaterialTheme.colorScheme.primary
                            },
                            onClick = DebugMenu::toggleVisibility,
                        ) {
                            Icon(
                                painter = painterResource(if (isVisible) Res.drawable.ic_debug_on else Res.drawable.ic_debug_off),
                                contentDescription = stringResource(Res.string.debug_menu),
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = isVisible,
            ) {
                theme {
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
                            logs = DebugMenu.logs.collectAsState(emptyList()).value,
                            onIsDebugOverlayEnabledChanged = DebugMenu::onIsDebugOverlayEnabledChanged,
                        )
                    }
                }
            }
        }
    } else {
        gameCanvas()
    }
}
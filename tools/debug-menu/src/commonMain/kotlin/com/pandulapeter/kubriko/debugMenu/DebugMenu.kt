package com.pandulapeter.kubriko.debugMenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme
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
    internal val persistenceManager = PersistenceManager.newInstance(fileName = "kubrikoDebugMenu")
    private val _isVisible = persistenceManager.boolean(
        key = "isVisible",
        defaultValue = false,
    )
    val isVisible = _isVisible.asStateFlow()
    private val _isDebugOverlayEnabled = persistenceManager.boolean(
        key = "isDebugOverlayEnabled",
        defaultValue = false,
    )
    internal val isDebugOverlayEnabled = _isDebugOverlayEnabled.asStateFlow()
    private val _isLowPriorityEnabled = persistenceManager.boolean(
        key = "isLowPriorityEnabled",
        defaultValue = true,
    )
    internal val isLowPriorityEnabled = _isLowPriorityEnabled.asStateFlow()
    private val _isMediumPriorityEnabled = persistenceManager.boolean(
        key = "isMediumPriorityEnabled",
        defaultValue = true,
    )
    internal val isMediumPriorityEnabled = _isMediumPriorityEnabled.asStateFlow()
    private val _isHighPriorityEnabled = persistenceManager.boolean(
        key = "isHighPriorityEnabled",
        defaultValue = true,
    )
    internal val isHighPriorityEnabled = _isHighPriorityEnabled.asStateFlow()
    private val _filter = persistenceManager.string(
        key = "filter",
        defaultValue = "",
    )
    val filter = _filter.asStateFlow()
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

    fun toggleVisibility() {
        _isVisible.value = !isVisible.value
    }

    internal fun onIsDebugOverlayEnabledChanged() = _isDebugOverlayEnabled.update { !it }

    internal fun onLowPriorityToggled() = _isLowPriorityEnabled.update { !it }

    internal fun onMediumPriorityToggled() = _isMediumPriorityEnabled.update { !it }

    internal fun onHighPriorityToggled() = _isHighPriorityEnabled.update { !it }

    fun onFilterUpdated(newFilter: String) = _filter.update { newFilter }
}

/**
 * TODO: Documentation
 */
@Composable
fun DebugMenu(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    kubriko: Kubriko?,
    isEnabled: Boolean = true,
    buttonAlignment: Alignment? = Alignment.TopStart,
    applyTheme: @Composable (@Composable () -> Unit) -> Unit = { KubrikoTheme(it) },
    gameCanvas: @Composable BoxScope.() -> Unit,
) = BoxWithConstraints(
    modifier = modifier,
) {
    if (isEnabled && kubriko != null) {
        val debugMenuManager = remember(kubriko) { DebugMenuManager(kubriko) }
        val debugMenuKubriko = remember(debugMenuManager) {
            Kubriko.newInstance(
                kubriko.get<ViewportManager>(),
                debugMenuManager,
                DebugMenu.persistenceManager,
            )
        }
        val isVisible = DebugMenu.isVisible.collectAsState().value
        val isColumn = maxWidth < maxHeight
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier.weight(1f),
            ) {
                GameContainer(
                    modifier = Modifier.weight(1f),
                    gameCanvas = gameCanvas,
                    debugMenuKubriko = debugMenuKubriko,
                    buttonAlignment = buttonAlignment,
                    windowInsets = windowInsets,
                    isVisible = isVisible,
                )
                AnimatedVisibility(
                    visible = isVisible && !isColumn,
                ) {
                    DebugMenuContainer(
                        modifier = Modifier
                            .defaultMinSize(
                                minWidth = 180.dp + windowInsets.only(WindowInsetsSides.Right).asPaddingValues()
                                    .calculateRightPadding(LocalLayoutDirection.current)
                            ).fillMaxHeight(),
                        debugMenuManager = debugMenuManager,
                        windowInsets = windowInsets,
                        shouldUseVerticalLayout = true,
                        applyTheme = applyTheme,
                    )
                }
            }
            AnimatedVisibility(
                visible = isVisible && isColumn,
            ) {
                Box(
                    modifier = Modifier.height(
                        160.dp + windowInsets.asPaddingValues().calculateBottomPadding()
                    ),
                ) {
                    DebugMenuContainer(
                        modifier = Modifier.fillMaxWidth(),
                        debugMenuManager = debugMenuManager,
                        windowInsets = windowInsets,
                        shouldUseVerticalLayout = false,
                        applyTheme = applyTheme,
                    )
                }
            }
        }
    } else {
        gameCanvas()
    }
}

@Composable
private fun GameContainer(
    modifier: Modifier,
    gameCanvas: @Composable BoxScope.() -> Unit,
    windowInsets: WindowInsets,
    debugMenuKubriko: Kubriko,
    buttonAlignment: Alignment?,
    isVisible: Boolean,
) = Box(
    modifier = modifier,
) {
    gameCanvas()
    KubrikoViewport(
        modifier = Modifier.windowInsetsPadding(windowInsets),
        kubriko = debugMenuKubriko,
    )
    if (buttonAlignment != null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
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

@Composable
private fun DebugMenuContainer(
    modifier: Modifier,
    debugMenuManager: DebugMenuManager,
    windowInsets: WindowInsets,
    shouldUseVerticalLayout: Boolean,
    applyTheme: @Composable (@Composable () -> Unit) -> Unit,
) = applyTheme {
    Surface(
        modifier = modifier,
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
            windowInsets = windowInsets,
            debugMenuMetadata = debugMenuManager.debugMenuMetadata.collectAsState(DebugMenuMetadata()).value,
            logs = DebugMenu.logs.collectAsState(emptyList()).value,
            onIsDebugOverlayEnabledChanged = DebugMenu::onIsDebugOverlayEnabledChanged,
            shouldUseVerticalLayout = shouldUseVerticalLayout,
        )
    }
}
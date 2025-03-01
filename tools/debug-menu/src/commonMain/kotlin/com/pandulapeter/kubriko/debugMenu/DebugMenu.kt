/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.debugMenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuContainer
import com.pandulapeter.kubriko.debugMenu.implementation.InternalDebugMenu
import kubriko.tools.debug_menu.generated.resources.Res
import kubriko.tools.debug_menu.generated.resources.debug_menu
import kubriko.tools.debug_menu.generated.resources.ic_debug_off
import kubriko.tools.debug_menu.generated.resources.ic_debug_on
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

object DebugMenu : DebugMenuContract {

    override val isVisible = InternalDebugMenu.isVisible

    override fun toggleVisibility() = InternalDebugMenu.toggleVisibility()

    @Composable
    override operator fun invoke(
        modifier: Modifier,
        windowInsets: WindowInsets,
        kubriko: Kubriko?,
        isEnabled: Boolean,
        buttonAlignment: Alignment?,
        debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit,
        kubrikoViewport: @Composable () -> Unit,
        verticalDebugMenuWidth: Dp,
        horizontalDebugMenuHeight: Dp,
    ) = BoxWithConstraints(
        modifier = modifier,
    ) {
        if (kubriko != null && isEnabled) {
            val isColumn = maxWidth < maxHeight
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                ) {
                    OverlayOnly(
                        modifier = Modifier.weight(1f),
                        kubriko = kubriko,
                        kubrikoViewport = kubrikoViewport,
                        buttonAlignment = buttonAlignment,
                    )
                    Vertical(
                        modifier = Modifier,
                        kubriko = kubriko,
                        isEnabled = !isColumn,
                        windowInsets = windowInsets,
                        debugMenuTheme = debugMenuTheme,
                        width = verticalDebugMenuWidth,
                    )
                }
                Horizontal(
                    modifier = Modifier,
                    kubriko = kubriko,
                    isEnabled = isColumn,
                    windowInsets = windowInsets,
                    debugMenuTheme = debugMenuTheme,
                    height = horizontalDebugMenuHeight,
                )
            }
        } else {
            kubrikoViewport()
        }
    }

    @Composable
    override fun Horizontal(
        modifier: Modifier,
        kubriko: Kubriko?,
        isEnabled: Boolean,
        windowInsets: WindowInsets,
        debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit,
        height: Dp,
    ) = AnimatedVisibility(
        visible = isEnabled && isVisible.collectAsState().value,
        enter = expandIn() + fadeIn(),
        exit = fadeOut() + shrinkOut(),
    ) {
        Box(
            modifier = modifier
                .height(height + windowInsets.asPaddingValues().calculateBottomPadding())
                .fillMaxWidth(),
        ) {
            DebugMenuContainer(
                modifier = Modifier.fillMaxWidth(),
                kubriko = kubriko,
                windowInsets = windowInsets,
                shouldUseVerticalLayout = false,
                debugMenuTheme = debugMenuTheme,
            )
        }
    }

    @Composable
    override fun Vertical(
        modifier: Modifier,
        kubriko: Kubriko?,
        isEnabled: Boolean,
        windowInsets: WindowInsets,
        debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit,
        width: Dp,
    ) = AnimatedVisibility(
        visible = isEnabled && isVisible.collectAsState().value,
        enter = expandIn() + fadeIn(),
        exit = fadeOut() + shrinkOut(),
    ) {
        DebugMenuContainer(
            modifier = modifier
                .width(width + windowInsets.only(WindowInsetsSides.Right).asPaddingValues().calculateRightPadding(LocalLayoutDirection.current))
                .fillMaxHeight(),
            kubriko = kubriko,
            windowInsets = windowInsets,
            shouldUseVerticalLayout = true,
            debugMenuTheme = debugMenuTheme,
        )
    }

    @Composable
    override fun OverlayOnly(
        modifier: Modifier,
        kubriko: Kubriko?,
        kubrikoViewport: @Composable () -> Unit,
        buttonAlignment: Alignment?,
    ) {
        DisposableEffect(kubriko) {
            InternalDebugMenu.setGameKubriko(kubriko)
            onDispose {
                InternalDebugMenu.clearGameKubriko(kubriko)
            }
        }
        Box {
            // We only need this to initialize the PersistenceManager of InternalDebugMenu so that user settings can get restored.
            KubrikoViewport(
                modifier = Modifier.size(0.dp),
                kubriko = InternalDebugMenu.internalKubriko,
            )
            kubrikoViewport()
            val debugMenuKubriko = InternalDebugMenu.debugMenuKubriko.collectAsState().value[kubriko?.instanceName]
            if (debugMenuKubriko != null) {
                KubrikoViewport(
                    modifier = modifier,
                    kubriko = debugMenuKubriko,
                )
            }
            if (buttonAlignment != null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                ) {
                    val isDebugMenuVisible = isVisible.collectAsState().value
                    FloatingActionButton(
                        modifier = Modifier.size(40.dp).align(buttonAlignment),
                        containerColor = if (isSystemInDarkTheme()) {
                            if (isDebugMenuVisible) MaterialTheme.colorScheme.primary else FloatingActionButtonDefaults.containerColor
                        } else {
                            if (isDebugMenuVisible) FloatingActionButtonDefaults.containerColor else MaterialTheme.colorScheme.primary
                        },
                        onClick = ::toggleVisibility,
                    ) {
                        Icon(
                            painter = painterResource(if (isDebugMenuVisible) Res.drawable.ic_debug_on else Res.drawable.ic_debug_off),
                            contentDescription = stringResource(Res.string.debug_menu),
                        )
                    }
                }
            }
        }
    }
}
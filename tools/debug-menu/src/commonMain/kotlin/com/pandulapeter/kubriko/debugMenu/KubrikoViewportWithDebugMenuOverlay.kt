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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.implementation.InternalDebugMenu
import kubriko.tools.debug_menu.generated.resources.Res
import kubriko.tools.debug_menu.generated.resources.debug_menu
import kubriko.tools.debug_menu.generated.resources.ic_debug_off
import kubriko.tools.debug_menu.generated.resources.ic_debug_on
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun KubrikoViewportWithDebugMenuOverlay(
    modifier: Modifier = Modifier,
    kubriko: Kubriko?,
    kubrikoViewport: @Composable () -> Unit,
    buttonAlignment: Alignment? = Alignment.TopStart,
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
                val isDebugMenuVisible = DebugMenu.isVisible.collectAsState().value
                FloatingActionButton(
                    modifier = Modifier.size(40.dp).align(buttonAlignment),
                    containerColor = if (isSystemInDarkTheme()) {
                        if (isDebugMenuVisible) MaterialTheme.colorScheme.primary else FloatingActionButtonDefaults.containerColor
                    } else {
                        if (isDebugMenuVisible) FloatingActionButtonDefaults.containerColor else MaterialTheme.colorScheme.primary
                    },
                    onClick = DebugMenu::toggleVisibility,
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
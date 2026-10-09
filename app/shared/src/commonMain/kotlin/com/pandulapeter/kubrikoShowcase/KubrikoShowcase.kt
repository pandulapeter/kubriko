/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.deeplink
import com.pandulapeter.kubrikoShowcase.implementation.processDeeplink
import com.pandulapeter.kubrikoShowcase.implementation.ui.ResourceLoader
import com.pandulapeter.kubrikoShowcase.implementation.ui.ShowcaseContent
import com.pandulapeter.kubrikoShowcase.implementation.ui.getStateHolder
import kotlin.coroutines.cancellation.CancellationException

/**
 * @param onBackgroundColorChanged Called with the theme's surface color whenever it changes, for the shells whose window
 * shows a color of its own where the Showcase has not drawn yet: the desktop's, when it is resized faster than the
 * content is laid out again.
 */
@Composable
fun KubrikoShowcase(
    isInFullscreenMode: Boolean?,
    getIsInFullscreenMode: () -> Boolean?,
    onFullscreenModeToggled: () -> Unit,
    deeplink: String? = selectedShowcaseEntry.value.deeplink,
    onDestinationChanged: (String?) -> Unit = { selectedShowcaseEntry.value = it.processDeeplink() },
    onFirstFrameDrawn: () -> Unit = {},
    onBackgroundColorChanged: (Color) -> Unit = {},
) {
    LaunchedEffect(Unit) {
        // The second frame only resumes once the first one has been presented, so the host page's loading screen
        // is handed over to content that is already on screen rather than to an empty canvas.
        withFrameNanos {}
        withFrameNanos {}
        onFirstFrameDrawn()
    }
    val platformUriHandler = LocalUriHandler.current
    val uriHandler = remember(platformUriHandler) { SafeUriHandler(platformUriHandler) }
    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        KubrikoTheme(
            areResourcesLoaded = ResourceLoader.areResourcesLoaded() && ShowcaseEntry.entries.all { it.areResourcesLoaded() },
        ) {
            val backgroundColor = MaterialTheme.colorScheme.surface
            SideEffect { onBackgroundColorChanged(backgroundColor) }
            LaunchedEffect(deeplink) {
                selectedShowcaseEntry.value = deeplink.processDeeplink()
            }
            LaunchedEffect(selectedShowcaseEntry.value) {
                onDestinationChanged(selectedShowcaseEntry.value?.deeplink)
            }
            NavigationBackHandler(
                state = rememberNavigationEventState(NavigationEventInfo.None),
                isBackEnabled = selectedShowcaseEntry.value != null,
            ) {
                val activeStateHolder = selectedShowcaseEntry.value?.getStateHolder()
                try {
                    if (activeStateHolder?.navigateBack(
                            isInFullscreenMode = getIsInFullscreenMode() == true,
                            onFullscreenModeToggled = onFullscreenModeToggled,
                        ) == false
                    ) {
                        activeStateHolder.stopMusic()
                        selectedShowcaseEntry.value = null
                    }
                } catch (_: CancellationException) {
                }
            }
            BoxWithConstraints {
                val activeStateHolder = selectedShowcaseEntry.value?.getStateHolder()
                LaunchedEffect(activeStateHolder) {
                    activeStateHolder?.backNavigationIntent?.collect {
                        if (getIsInFullscreenMode() == true) {
                            onFullscreenModeToggled()
                        }
                        selectedShowcaseEntry.value = null
                    }
                }
                ShowcaseContent(
                    shouldUseCompactUi = maxWidth < 640.dp,
                    shouldUseWideSideMenu = maxWidth >= 1200.dp,
                    allShowcaseEntries = ShowcaseEntry.entries,
                    getSelectedShowcaseEntry = { selectedShowcaseEntry.value },
                    selectedShowcaseEntry = selectedShowcaseEntry.value,
                    onShowcaseEntrySelected = { showcaseEntry ->
                        if (showcaseEntry?.getStateHolder() != activeStateHolder) {
                            activeStateHolder?.stopMusic()
                            selectedShowcaseEntry.value = showcaseEntry
                        }
                    },
                    activeKubrikoInstance = activeStateHolder?.kubriko?.collectAsState(null)?.value,
                    isInFullscreenMode = isInFullscreenMode,
                    onFullscreenModeToggled = onFullscreenModeToggled,
                    isInfoPanelVisible = StateHolder.isInfoPanelVisible.value,
                    toggleInfoPanelVisibility = { StateHolder.isInfoPanelVisible.value = !StateHolder.isInfoPanelVisible.value },
                )
            }
        }
    }
}

private val selectedShowcaseEntry = mutableStateOf<ShowcaseEntry?>(null)

/**
 * Opens links through the platform's handler, ignoring the ones nothing on the device can open (a `mailto:` link
 * without a mail app, for example), for which the platform handlers throw.
 */
private class SafeUriHandler(
    private val platformUriHandler: UriHandler,
) : UriHandler {

    override fun openUri(uri: String) {
        try {
            platformUriHandler.openUri(uri)
        } catch (_: Exception) {
        }
    }
}
/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntryType
import com.pandulapeter.kubrikoShowcase.implementation.ui.ResourceLoader
import com.pandulapeter.kubrikoShowcase.implementation.ui.ShowcaseContent
import com.pandulapeter.kubrikoShowcase.implementation.ui.getStateHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KubrikoShowcase(
    isInFullscreenMode: Boolean?,
    getIsInFullscreenMode: () -> Boolean?,
    onFullscreenModeToggled: () -> Unit,
    webEscapePressEvent: Flow<Unit>? = null,
    deeplink: String? = selectedShowcaseEntry.value.deeplink,
    onDestinationChanged: (String?) -> Unit = { selectedShowcaseEntry.value = it.processDeeplink() },
) = KubrikoTheme(
    areResourcesLoaded = ResourceLoader.areResourcesLoaded() && ShowcaseEntry.entries.all { it.areResourcesLoaded() },
) {
    LaunchedEffect(deeplink) {
        selectedShowcaseEntry.value = deeplink.processDeeplink()
    }
    LaunchedEffect(selectedShowcaseEntry.value) {
        onDestinationChanged(selectedShowcaseEntry.value?.deeplink)
    }
    LaunchedEffect(webEscapePressEvent) {
        webEscapePressEvent?.collect {
            val activeStateHolder = selectedShowcaseEntry.value?.getStateHolder()
            if (activeStateHolder?.navigateBack(
                    isInFullscreenMode = isInFullscreenMode == true,
                    onFullscreenModeToggled = onFullscreenModeToggled,
                ) == false
            ) {
                activeStateHolder.stopMusic()
                selectedShowcaseEntry.value = null
            }
        }
    }
    BackHandler(selectedShowcaseEntry.value != null) {
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
        val scope = rememberCoroutineScope()
        LaunchedEffect(activeStateHolder) {
            activeStateHolder?.backNavigationIntent?.onEach {
                if (getIsInFullscreenMode() == true) {
                    onFullscreenModeToggled()
                }
                selectedShowcaseEntry.value = null
            }?.launchIn(scope)
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
            toggleInfoPanelVisibility = { StateHolder.isInfoPanelVisible.value = !StateHolder.isInfoPanelVisible.value }
        )
    }
}

private val ShowcaseEntry?.deeplink
    get() = when (this) {
        ShowcaseEntry.WALLBREAKER -> "wallbreaker"
        ShowcaseEntry.SPACE_SQUADRON -> "space-squadron"
        ShowcaseEntry.ANNOYED_PENGUINS -> "annoyed-penguins"
        ShowcaseEntry.BLOCKYS_JOURNEY -> "blockys-journey"
        ShowcaseEntry.CONTENT_SHADERS -> "content-shaders"
        ShowcaseEntry.PARTICLES -> "particles"
        ShowcaseEntry.PERFORMANCE -> "performance"
        ShowcaseEntry.PHYSICS -> "physics"
        ShowcaseEntry.SHADER_ANIMATIONS -> "shader-animations"
        ShowcaseEntry.AUDIO -> "audio"
        ShowcaseEntry.INPUT -> "input"
        ShowcaseEntry.ABOUT -> "about"
        ShowcaseEntry.LICENSES -> "licenses"
        else -> null
    }

private fun String?.processDeeplink() = this?.trim()?.lowercase()?.split("/")?.filterNot { it.isBlank() }?.lastOrNull().let { deeplink ->
    ShowcaseEntry.entries.firstOrNull { it.deeplink == deeplink }
}.let {
    if (it?.type == ShowcaseEntryType.TEST && !BuildConfig.ARE_TEST_EXAMPLES_ENABLED) null else it
}

private val selectedShowcaseEntry = mutableStateOf<ShowcaseEntry?>(null)
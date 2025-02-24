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
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ui.ShowcaseContent
import com.pandulapeter.kubrikoShowcase.implementation.ui.getStateHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KubrikoShowcase(
    isInFullscreenMode: Boolean,
    getIsInFullscreenMode: () -> Boolean,
    onFullscreenModeToggled: () -> Unit,
    webEscapePressEvent: Flow<Unit>? = null,
) = KubrikoTheme {
    LaunchedEffect(webEscapePressEvent) {
        webEscapePressEvent?.collect {
            val activeStateHolder = selectedShowcaseEntry.value?.getStateHolder()
            if (activeStateHolder?.navigateBack(
                isInFullscreenMode = isInFullscreenMode,
                onFullscreenModeToggled = onFullscreenModeToggled,
            ) == false) {
                activeStateHolder.stopMusic()
                selectedShowcaseEntry.value = null
            }
        }
    }
    BackHandler(selectedShowcaseEntry.value != null) {
        val activeStateHolder = selectedShowcaseEntry.value?.getStateHolder()
        try {
            if (activeStateHolder?.navigateBack(
                isInFullscreenMode = getIsInFullscreenMode(),
                onFullscreenModeToggled = onFullscreenModeToggled,
            ) == false) {
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
                selectedShowcaseEntry.value = null
            }?.launchIn(scope)
        }
        ShowcaseContent(
            shouldUseCompactUi = maxWidth <= 680.dp,
            allShowcaseEntries = ShowcaseEntry.entries,
            getSelectedShowcaseEntry = { selectedShowcaseEntry.value },
            selectedShowcaseEntry = selectedShowcaseEntry.value,
            onShowcaseEntrySelected = { showcaseEntry ->
                activeStateHolder?.stopMusic()
                selectedShowcaseEntry.value = showcaseEntry
            },
            activeKubrikoInstance = activeStateHolder?.kubriko?.collectAsState(null)?.value,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )
    }
}

private val selectedShowcaseEntry = mutableStateOf<ShowcaseEntry?>(null)
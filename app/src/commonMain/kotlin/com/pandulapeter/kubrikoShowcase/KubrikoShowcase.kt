/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ui.ShowcaseContent
import com.pandulapeter.kubrikoShowcase.implementation.ui.getStateHolder

@Composable
fun KubrikoShowcase(
    isInFullscreenMode: Boolean,
    onFullscreenModeToggled: () -> Unit,
) = KubrikoTheme {
    BoxWithConstraints {
        val activeStateHolder = selectedShowcaseEntry.value?.getStateHolder()
        ShowcaseContent(
            shouldUseCompactUi = maxWidth <= 720.dp,
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
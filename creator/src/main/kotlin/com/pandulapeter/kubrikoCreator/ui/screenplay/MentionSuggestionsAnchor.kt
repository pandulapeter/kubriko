/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.pandulapeter.kubrikoCreator.ui.components.Popover
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState

/**
 * [content], the field a mention is typed into, with [suggestions] for it in a popover below it while there are any,
 * following the field as it grows. The popover never takes the focus, so typing goes on in the field.
 */
@Composable
internal fun MentionSuggestionsAnchor(
    suggestions: MentionSuggestions?,
    highlighted: Int,
    onChoose: (MentionCandidate) -> Unit,
    content: @Composable () -> Unit,
) {
    val popover = rememberRightClickMenuState()
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var height by remember { mutableStateOf(0) }
    Box(
        modifier = Modifier.fillMaxWidth().onGloballyPositioned {
            coordinates = it
            height = it.size.height
        },
    ) {
        content()
    }
    val isShown = suggestions != null
    LaunchedEffect(isShown, height) {
        val anchor = coordinates
        if (isShown && anchor != null) popover.openBelow(anchor) else popover.close()
    }
    if (suggestions != null) {
        Popover(state = popover, isFocusable = false) {
            MentionSuggestionList(candidates = suggestions.matches, highlighted = highlighted, onChoose = onChoose)
        }
    }
}

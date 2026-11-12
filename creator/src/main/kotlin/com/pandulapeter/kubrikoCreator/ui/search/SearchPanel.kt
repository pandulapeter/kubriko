/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.SearchHit
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.ui.FocusRequest
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_search
import kubriko.creator.generated.resources.search_field
import kubriko.creator.generated.resources.search_title
import org.jetbrains.compose.resources.stringResource

/**
 * The Search panel: what refers to [usagesSubject] ([UsageResults]) while one is asked about, with a way back
 * ([onBackToSearch]); otherwise a field searching the project's texts ([TextResults]). Each result goes to its place
 * when clicked.
 *
 * @param fieldFocus Asks the field to take the focus (Cmd/Ctrl+Shift+F).
 */
@Composable
internal fun SearchPanel(
    usagesSubject: RelationSubject?,
    usages: UsagesState,
    onUsageSelected: (Usage) -> Unit,
    modifier: Modifier = Modifier,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    texts: TextSearchState = TextSearchState.Idle,
    onHitSelected: (SearchHit) -> Unit = {},
    onBackToSearch: () -> Unit = {},
    fieldFocus: FocusRequest = remember { FocusRequest() },
) = Column(
    modifier = modifier,
) {
    PanelHeader(
        icon = Res.drawable.ic_search,
        title = stringResource(Res.string.search_title),
    )
    HorizontalDivider()
    if (usagesSubject != null) {
        UsageResults(
            subject = usagesSubject,
            usages = usages,
            onUsageSelected = onUsageSelected,
            onBack = onBackToSearch,
        )
    } else {
        SearchField(query = query, onQueryChange = onQueryChange, focus = fieldFocus)
        TextResults(state = texts, onHitSelected = onHitSelected)
    }
}

/**
 * The field the project's texts are searched with, taking the focus when [focus] asks, and clearing on Escape.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focus: FocusRequest,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focus.isPending) {
        if (focus.isPending) {
            focusRequester.requestFocus()
            focus.consume()
        }
    }
    CompactTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                (event.type == KeyEventType.KeyDown && event.key == Key.Escape && query.isNotEmpty()).also { if (it) onQueryChange("") }
            },
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(Res.string.search_field),
    )
}

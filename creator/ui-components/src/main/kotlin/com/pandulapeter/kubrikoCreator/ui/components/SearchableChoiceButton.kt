/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.Res
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.choice_none_found
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.choice_search
import org.jetbrains.compose.resources.stringResource

/**
 * An outlined button showing the chosen option, which opens [options] in a panel: a lazy list, with a field filtering it
 * ([matchingOptions]) once there are more than [SEARCH_THRESHOLD], where Enter picks the first match and Escape closes.
 * Enter with nothing typed picks nothing, since the first option is often "none", which would clear the choice.
 *
 * @param isMissing Whether the chosen option no longer exists ([MenuButtonFace]).
 */
@Composable
public fun <T> SearchableChoiceButton(
    label: String,
    options: List<T>,
    optionText: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    isMissing: Boolean = false,
): Unit = Box {
    var isExpanded by remember { mutableStateOf(false) }
    MenuButtonFace(
        label = label,
        onClick = { isExpanded = true },
        isMissing = isMissing,
    )
    if (isExpanded) {
        Popup(
            onDismissRequest = { isExpanded = false },
            properties = PopupProperties(focusable = true),
        ) {
            val titles = options.map { optionText(it) }
            ChoicePanel(
                label = label,
                titles = titles,
                onSelected = { index ->
                    isExpanded = false
                    onSelected(options[index])
                },
                onDismissed = { isExpanded = false },
            )
        }
    }
}

/**
 * How many options a [SearchableChoiceButton] lists before it offers a field to filter them.
 */
internal const val SEARCH_THRESHOLD = 8

@Composable
private fun ChoicePanel(
    label: String,
    titles: List<String>,
    onSelected: (index: Int) -> Unit,
    onDismissed: () -> Unit,
) = Surface(
    modifier = Modifier.width(260.dp),
    shape = MenuStyle.shape,
    color = MenuStyle.color,
    border = MenuStyle.border,
    shadowElevation = MenuStyle.elevation,
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(query, titles) { matchingOptions(query, titles.indices.toList(), titleOf = { titles[it] }) }
    Column {
        if (titles.size > SEARCH_THRESHOLD) {
            ChoiceSearchField(
                query = query,
                onQueryChange = { query = it },
                onConfirmed = { if (query.isNotBlank()) matches.firstOrNull()?.let(onSelected) },
                onDismissed = onDismissed,
            )
        }
        if (matches.isEmpty()) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                text = stringResource(Res.string.choice_none_found),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier.fillMaxWidth().scrollEdgeFades(listState),
            state = listState,
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(matches, key = { it }) { index ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(index) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    text = titles[index],
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (titles[index] == label) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ChoiceSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    CompactTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 8.dp, bottom = 4.dp)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter, Key.NumPadEnter -> {
                        onConfirmed()
                        true
                    }
                    Key.Escape -> {
                        onDismissed()
                        true
                    }
                    else -> false
                }
            },
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(Res.string.choice_search),
    )
}

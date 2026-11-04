/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.composeKeyNameOf
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.MenuButtonFace
import com.pandulapeter.kubrikoCreator.ui.components.MenuStyle
import com.pandulapeter.kubrikoCreator.ui.components.matchingOptions
import com.pandulapeter.kubrikoCreator.ui.components.scrollEdgeFades
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_keyboard
import kubriko.creator.generated.resources.key_listening
import kubriko.creator.generated.resources.key_none_found
import kubriko.creator.generated.resources.key_press
import kubriko.creator.generated.resources.key_search
import org.jetbrains.compose.resources.stringResource

/**
 * Which key an Event or a condition reacts to: a button showing it, which opens every key Compose knows, the common ones
 * first, filtered by what is typed, or chosen by pressing it.
 */
@Composable
internal fun KeyChoiceButton(
    key: KeyChoice,
    onSelected: (KeyChoice) -> Unit,
) = Box {
    var isExpanded by remember { mutableStateOf(false) }
    MenuButtonFace(
        label = key.text(),
        onClick = { isExpanded = true },
    )
    if (isExpanded) {
        Popup(
            onDismissRequest = { isExpanded = false },
            properties = PopupProperties(focusable = true),
        ) {
            KeyChoicePanel(
                onSelected = {
                    isExpanded = false
                    onSelected(it)
                },
            )
        }
    }
}

@Composable
private fun KeyChoicePanel(
    onSelected: (KeyChoice) -> Unit,
) = Surface(
    modifier = Modifier.width(260.dp),
    shape = MenuStyle.shape,
    color = MenuStyle.color,
    border = MenuStyle.border,
    shadowElevation = MenuStyle.elevation,
) {
    var query by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val commonTitles = KeyChoice.common.associateWith { it.text() }
    val keys = remember(query, commonTitles) { matchingKeys(query) { commonTitles[it] ?: readableKeyName(it.composeKeyName) } }
    LaunchedEffect(isListening) { focusRequester.requestFocus() }
    Column {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isListening) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) composeKeyNameOf(event.key)?.let { onSelected(KeyChoice(it)) }
                            true
                        },
                    text = stringResource(Res.string.key_listening),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                CompactTextField(
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(Res.string.key_search),
                )
            }
            IconActionButton(
                icon = Res.drawable.ic_keyboard,
                label = Res.string.key_press,
                onClick = { isListening = !isListening },
                tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (keys.isEmpty()) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                text = stringResource(Res.string.key_none_found),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier.fillMaxWidth().scrollEdgeFades(listState, MenuStyle.color),
            state = listState,
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(keys, key = { it.composeKeyName }) { key ->
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(key) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    text = commonTitles[key] ?: readableKeyName(key.composeKeyName),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * Every key whose title (from [titleOf]) or Compose name contains [query], those it matches exactly first, then those
 * starting with it, otherwise in the order of [KeyChoice.all].
 */
internal fun matchingKeys(query: String, titleOf: (KeyChoice) -> String) = matchingOptions(query, KeyChoice.all, titleOf, alsoMatching = { it.composeKeyName })

/**
 * A Compose key name made readable, with words apart: `MediaPlayPause` is "Media Play Pause", `NumPad5` "Num Pad5".
 */
internal fun readableKeyName(composeKeyName: String) = composeKeyName.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")

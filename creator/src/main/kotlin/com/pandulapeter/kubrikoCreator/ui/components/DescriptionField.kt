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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.description_hint
import kubriko.creator.generated.resources.description_hint_without_ai
import kubriko.creator.generated.resources.description_label
import org.jetbrains.compose.resources.stringResource

/**
 * The one-line description of a type or a Scene, saying what it is for, for the user and the AI. What is typed shows at
 * once, even where the change reaches the model later; a description changed elsewhere (by the AI or by undo) shows as
 * soon as the field isn't being typed into.
 *
 * @param key What is described, so that another one starts from its own description.
 */
@Composable
internal fun DescriptionField(
    key: Any,
    description: String?,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(key) { mutableStateOf(description.orEmpty()) }
    var isFocused by remember(key) { mutableStateOf(false) }
    LaunchedEffect(description, isFocused) {
        if (!isFocused) text = description.orEmpty()
    }
    CompactTextField(
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        value = text,
        onValueChange = { value ->
            val line = value.replace('\n', ' ')
            text = line
            onDescriptionChange(line)
        },
        label = stringResource(Res.string.description_label),
        placeholder = stringResource(if (LocalIsAiEnabled.current) Res.string.description_hint else Res.string.description_hint_without_ai),
    )
}

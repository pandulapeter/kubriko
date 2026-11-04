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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.Res
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.description_hint
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.description_hint_without_ai
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.description_label
import org.jetbrains.compose.resources.stringResource

/**
 * The description of a type or a Scene, saying what it is for, for the user and the AI: one line looking like a field,
 * ending in an ellipsis when it is longer, which opens a dialog to edit it in. The dialog saves once, through
 * [onDescriptionChange], so one edit is one step of the undo history; a description changed elsewhere (by the AI or by
 * undo) shows at once.
 *
 * @param name What is described, named in the dialog's title.
 * @param maximumLength How long the description may be.
 * @param action What the row offers at its end besides editing, such as having the AI write the description.
 */
@Composable
public fun DescriptionPreview(
    name: String,
    description: String?,
    maximumLength: Int,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    var isEditing by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val placeholder = stringResource(if (LocalIsAiEnabled.current) Res.string.description_hint else Res.string.description_hint_without_ai)
    Row(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(MaterialTheme.shapes.small)
            .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.small)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { isEditing = true }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.description_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = description ?: placeholder,
            style = MaterialTheme.typography.bodyMedium,
            color = if (description == null) colors.onSurfaceVariant else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        action()
    }
    if (isEditing) {
        DescriptionDialog(
            name = name,
            initialDescription = description.orEmpty(),
            placeholder = placeholder,
            maximumLength = maximumLength,
            onSave = { text ->
                isEditing = false
                if (text != description.orEmpty()) onDescriptionChange(text)
            },
            onDismiss = { isEditing = false },
        )
    }
}

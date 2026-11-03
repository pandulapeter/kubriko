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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.Res
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.action_add
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.action_cancel
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.ic_check
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.ic_close
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Names something new (an Actor type, a Scene, a folder), or names something again: Enter creates or renames it, Escape
 * cancels, and a rejected name shows why.
 *
 * @param initialName The name it starts with, for renaming.
 * @param confirmLabel What its confirming button is called.
 * @param icon The icon of what it names, so it looks like the row it becomes, in [iconTint] (the muted content color by default).
 * @param indent How far it is from the start, to line up with the rows around it.
 */
@Composable
public fun NewNameField(
    placeholder: StringResource,
    onCreate: (String) -> FieldError?,
    onCancel: () -> Unit,
    initialName: String = "",
    indent: Dp = 24.dp,
    confirmLabel: StringResource = Res.string.action_add,
    icon: DrawableResource? = null,
    iconTint: Color = Color.Unspecified,
): Unit = Column(
    modifier = Modifier.fillMaxWidth().padding(start = indent, end = 4.dp, bottom = 4.dp),
) {
    var name by remember { mutableStateOf(initialName) }
    var error by remember { mutableStateOf<FieldError?>(null) }
    val focusRequester = remember { FocusRequester() }
    val create = { if (name.isNotBlank()) error = onCreate(name.trim()) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.padding(end = 6.dp).size(16.dp),
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant },
            )
        }
        CompactTextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    when {
                        event.type != KeyEventType.KeyDown -> false
                        event.key == Key.Enter -> true.also { create() }
                        event.key == Key.Escape -> true.also { onCancel() }
                        else -> false
                    }
                },
            value = name,
            onValueChange = {
                name = it
                error = null
            },
            placeholder = stringResource(placeholder),
            isError = error != null,
        )
        IconActionButton(
            icon = Res.drawable.ic_check,
            label = confirmLabel,
            onClick = create,
            isEnabled = name.isNotBlank(),
        )
        IconActionButton(
            icon = Res.drawable.ic_close,
            label = Res.string.action_cancel,
            onClick = onCancel,
        )
    }
    error?.let {
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = stringResource(it.text, *it.arguments.toTypedArray()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import com.pandulapeter.kubrikoCreator.ui.components.FieldErrorText
import com.pandulapeter.kubrikoCreator.ui.components.NumberField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.new_sprite_create
import kubriko.creator.generated.resources.new_sprite_name
import kubriko.creator.generated.resources.new_sprite_title
import kubriko.creator.generated.resources.sprite_operation_height
import kubriko.creator.generated.resources.sprite_operation_width
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for a new sprite's name and size (32 by 32 pixels unless changed), and makes it through [onCreate], which returns
 * why it was refused, shown under the fields, or null once it is made.
 */
@Composable
internal fun NewSpriteDialog(
    onCreate: (name: String, width: Int, height: Int) -> Rejection?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var width by remember { mutableIntStateOf(DEFAULT_SIZE) }
    var height by remember { mutableIntStateOf(DEFAULT_SIZE) }
    var rejection by remember { mutableStateOf<Rejection?>(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    CreatorDialog(
        title = stringResource(Res.string.new_sprite_title),
        maxWidth = 400.dp,
        onDismiss = onDismiss,
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { rejection = onCreate(name.trim(), width, height).also { if (it == null) onDismiss() } },
            ) { Text(stringResource(Res.string.new_sprite_create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactTextField(
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                value = name,
                onValueChange = {
                    name = it
                    rejection = null
                },
                label = stringResource(Res.string.new_sprite_name),
                isError = rejection != null,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SizeField(stringResource(Res.string.sprite_operation_width), width) { width = it }
                SizeField(stringResource(Res.string.sprite_operation_height), height) { height = it }
            }
            rejection?.let { FieldErrorText(it.toFieldError()) }
        }
    }
}

@Composable
private fun SizeField(label: String, value: Int, onValueChange: (Int) -> Unit) = NumberField(
    modifier = Modifier.width(150.dp),
    label = label,
    value = value.toFloat(),
    onValueChange = { if (it == it.toInt().toFloat()) onValueChange(it.toInt()) },
)

private const val DEFAULT_SIZE = 32

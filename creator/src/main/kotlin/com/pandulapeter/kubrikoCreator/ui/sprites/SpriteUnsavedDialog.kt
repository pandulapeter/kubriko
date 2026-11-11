/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.sprites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.actorTypes.spriteLabel
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_dont_save
import kubriko.creator.generated.resources.action_save
import kubriko.creator.generated.resources.sprite_editor_unsaved_message
import kubriko.creator.generated.resources.sprite_editor_unsaved_title
import org.jetbrains.compose.resources.stringResource

/**
 * Asks what becomes of the changes the sprite editor holds of the sprite at [path] before it shows something else.
 */
@Composable
internal fun SpriteUnsavedDialog(
    path: String,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) = CreatorDialog(
    title = stringResource(Res.string.sprite_editor_unsaved_title, spriteLabel(path)),
    maxWidth = 480.dp,
    onDismiss = onDismiss,
    text = { Text(stringResource(Res.string.sprite_editor_unsaved_message)) },
    confirmButton = { Button(onClick = onSave) { Text(stringResource(Res.string.action_save)) } },
    dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
            TextButton(onClick = onDiscard) { Text(stringResource(Res.string.action_dont_save)) }
        }
    },
)

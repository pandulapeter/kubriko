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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.ui.components.rememberImageFile
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_sprite_frame_summary
import kubriko.creator.generated.resources.actor_type_sprite_frames
import kubriko.creator.generated.resources.actor_type_sprite_whole
import org.jetbrains.compose.resources.stringResource

/**
 * The sprite at [path] as an Actor type is drawn as it, playing if it is cut into frames, with how it is cut and a
 * button that opens [SpriteSheetDialog] to change it.
 *
 * @param tint The color the sprite is multiplied by, if it is tinted.
 * @param onSheetChange Called with how to cut the sprite, or null to show it whole, and the image's size once it is read.
 */
@Composable
internal fun SpriteFramesRow(
    path: String,
    sprites: SpriteChoices,
    tint: Color?,
    onSheetChange: (SpriteSheet?, imageSize: IntSize?) -> Unit,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
) {
    val bitmap = rememberImageFile(sprites.fileOf(path))
    val sheet = sprites.sheetOf(path)
    var isEditing by remember(path) { mutableStateOf(false) }
    val frame = rememberPlayingFrame(sheet)
    SpriteThumbnail(
        modifier = Modifier.size(48.dp),
        bitmap = bitmap,
        sheet = sheet,
        frame = { frame.value },
        tint = tint,
    )
    Column {
        Text(
            text = sheet?.let { stringResource(Res.string.actor_type_sprite_frame_summary, it.frameCount, formatFramesPerSecond(it.framesPerSecond)) }
                ?: stringResource(Res.string.actor_type_sprite_whole),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { isEditing = true }) { Text(stringResource(Res.string.actor_type_sprite_frames)) }
    }
    if (isEditing) {
        SpriteSheetDialog(
            path = path,
            bitmap = bitmap,
            initialSheet = sheet,
            onSave = {
                isEditing = false
                if (it != sheet) onSheetChange(it, bitmap?.let { image -> IntSize(image.width, image.height) })
            },
            onDismiss = { isEditing = false },
        )
    }
}

internal fun formatFramesPerSecond(value: Float) = if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()

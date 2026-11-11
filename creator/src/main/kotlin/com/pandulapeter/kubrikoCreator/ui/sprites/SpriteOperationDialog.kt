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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.resources.EditSprite
import com.pandulapeter.kubrikoCreator.sprites.pixels.CanvasAnchor
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import com.pandulapeter.kubrikoCreator.ui.components.NumberField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.sprite_operation_anchor
import kubriko.creator.generated.resources.sprite_operation_apply
import kubriko.creator.generated.resources.sprite_operation_factor
import kubriko.creator.generated.resources.sprite_operation_height
import kubriko.creator.generated.resources.sprite_operation_shift_x
import kubriko.creator.generated.resources.sprite_operation_shift_y
import kubriko.creator.generated.resources.sprite_operation_width
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for the numbers of [action] (one that [SpriteImageAction.isAsking]): how far to shift, the canvas's new size and
 * what keeps its place, or how much to scale, starting from a frame of [frameWidth] by [frameHeight] pixels.
 */
@Composable
internal fun SpriteOperationDialog(
    action: SpriteImageAction,
    frameWidth: Int,
    frameHeight: Int,
    onApply: (EditSprite.Operation) -> Unit,
    onDismiss: () -> Unit,
) {
    var first by remember { mutableIntStateOf(if (action == SpriteImageAction.RESIZE_CANVAS) frameWidth else if (action == SpriteImageAction.SCALE) 2 else 1) }
    var second by remember { mutableIntStateOf(if (action == SpriteImageAction.RESIZE_CANVAS) frameHeight else 0) }
    var anchor by remember { mutableStateOf(CanvasAnchor.CENTER) }
    val operation = when (action) {
        SpriteImageAction.SHIFT -> EditSprite.Operation.Shift(first, second)
        SpriteImageAction.RESIZE_CANVAS -> EditSprite.Operation.ResizeCanvas(first, second, anchor).takeIf { first in 1..EditSprite.MAXIMUM_SIZE && second in 1..EditSprite.MAXIMUM_SIZE }
        SpriteImageAction.SCALE -> EditSprite.Operation.Scale(first).takeIf { first in 1..MAXIMUM_SCALE }
        else -> action.operation
    }
    CreatorDialog(
        title = stringResource(action.title).removeSuffix("…"),
        maxWidth = 420.dp,
        onDismiss = onDismiss,
        confirmButton = { TextButton(enabled = operation != null, onClick = { operation?.let(onApply) }) { Text(stringResource(Res.string.sprite_operation_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (action) {
                SpriteImageAction.SHIFT -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WholeNumber(stringResource(Res.string.sprite_operation_shift_x), first) { first = it }
                    WholeNumber(stringResource(Res.string.sprite_operation_shift_y), second) { second = it }
                }
                SpriteImageAction.RESIZE_CANVAS -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WholeNumber(stringResource(Res.string.sprite_operation_width), first) { first = it }
                        WholeNumber(stringResource(Res.string.sprite_operation_height), second) { second = it }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.sprite_operation_anchor), style = MaterialTheme.typography.bodySmall)
                        MenuButton(
                            label = stringResource(anchor.title),
                            options = CanvasAnchor.entries,
                            optionText = { stringResource(it.title) },
                            onSelected = { anchor = it },
                        )
                    }
                }
                SpriteImageAction.SCALE -> WholeNumber(stringResource(Res.string.sprite_operation_factor), first) { first = it }
                else -> Unit
            }
        }
    }
}

@Composable
private fun WholeNumber(label: String, value: Int, onValueChange: (Int) -> Unit) = NumberField(
    modifier = Modifier.width(150.dp),
    label = label,
    value = value.toFloat(),
    onValueChange = { if (it == it.toInt().toFloat()) onValueChange(it.toInt()) },
)

private const val MAXIMUM_SCALE = 16

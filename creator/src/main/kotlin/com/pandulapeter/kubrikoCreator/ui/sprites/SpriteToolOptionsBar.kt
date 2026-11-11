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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.sprite_editor_contiguous
import kubriko.creator.generated.resources.sprite_editor_filled
import org.jetbrains.compose.resources.stringResource

/**
 * The tool chosen, and its options beside it, in a bar of their own across the editor, so a switch's label has room.
 * It keeps its height whatever the tool, so the canvas under it never moves.
 */
@Composable
internal fun SpriteToolOptionsBar(
    state: SpriteEditorState,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier.fillMaxWidth().height(36.dp).padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
) {
    Text(
        modifier = Modifier.weight(1f, fill = false),
        text = stringResource(state.tool.title),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    when (state.tool) {
        SpriteTool.RECTANGLE, SpriteTool.ELLIPSE -> LabeledSwitch(
            label = stringResource(Res.string.sprite_editor_filled),
            checked = state.isShapeFilled,
            onCheckedChange = { state.isShapeFilled = it },
        )
        SpriteTool.FILL -> LabeledSwitch(
            label = stringResource(Res.string.sprite_editor_contiguous),
            checked = state.isFillContiguous,
            onCheckedChange = { state.isFillContiguous = it },
        )
        SpriteTool.PENCIL, SpriteTool.ERASER, SpriteTool.LINE, SpriteTool.PICKER, SpriteTool.SELECT -> Unit
    }
}

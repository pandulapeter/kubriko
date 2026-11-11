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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.sprite_editor_contiguous
import kubriko.creator.generated.resources.sprite_editor_filled
import org.jetbrains.compose.resources.stringResource

/**
 * The sprite editor's tools, the chosen one marked, with the options of the chosen one under them.
 */
@Composable
internal fun SpriteToolColumn(
    state: SpriteEditorState,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.padding(6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp),
) {
    SpriteTool.entries.forEach { tool ->
        IconActionButton(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (state.tool == tool) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
            icon = tool.icon,
            label = tool.title,
            onClick = { state.tool = tool },
        )
    }
    when (state.tool) {
        SpriteTool.RECTANGLE, SpriteTool.ELLIPSE -> LabeledSwitch(
            modifier = Modifier.width(OPTION_WIDTH),
            label = stringResource(Res.string.sprite_editor_filled),
            checked = state.isShapeFilled,
            onCheckedChange = { state.isShapeFilled = it },
        )
        SpriteTool.FILL -> LabeledSwitch(
            modifier = Modifier.width(OPTION_WIDTH),
            label = stringResource(Res.string.sprite_editor_contiguous),
            checked = state.isFillContiguous,
            onCheckedChange = { state.isFillContiguous = it },
        )
        SpriteTool.PENCIL, SpriteTool.ERASER, SpriteTool.LINE, SpriteTool.PICKER, SpriteTool.SELECT -> Unit
    }
}

private val OPTION_WIDTH = 96.dp

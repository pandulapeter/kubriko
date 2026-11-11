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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.actorTypes.spriteLabel
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_chevron
import kubriko.creator.generated.resources.ic_fit_screen
import kubriko.creator.generated.resources.ic_zoom_in
import kubriko.creator.generated.resources.ic_zoom_out
import kubriko.creator.generated.resources.sprite_editor_every_frame
import kubriko.creator.generated.resources.sprite_editor_frame
import kubriko.creator.generated.resources.sprite_editor_image
import kubriko.creator.generated.resources.sprite_editor_next_frame
import kubriko.creator.generated.resources.sprite_editor_previous_frame
import kubriko.creator.generated.resources.sprite_editor_zoom
import kubriko.creator.generated.resources.sprite_editor_zoom_fit
import kubriko.creator.generated.resources.sprite_editor_zoom_in
import kubriko.creator.generated.resources.sprite_editor_zoom_out
import org.jetbrains.compose.resources.stringResource

/**
 * Above the sprite editor's canvas: the sprite's name, the frame shown (stepped through for a sprite cut into frames),
 * the zoom, and the Image menu, whose operations change the frame shown or, by the switch, every frame.
 */
@Composable
internal fun SpriteEditorToolbar(
    path: String,
    frameCount: Int,
    state: SpriteEditorState,
    onImageAction: (SpriteImageAction) -> Unit,
    onFit: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(
        modifier = Modifier.weight(1f, fill = false),
        text = spriteLabel(path),
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    if (frameCount > 1) {
        IconActionButton(
            modifier = Modifier.rotate(90f),
            icon = Res.drawable.ic_chevron,
            label = Res.string.sprite_editor_previous_frame,
            onClick = { state.frameIndex = (state.frameIndex - 1).mod(frameCount) },
        )
        Text(stringResource(Res.string.sprite_editor_frame, state.frameIndex + 1, frameCount), style = MaterialTheme.typography.bodySmall)
        IconActionButton(
            modifier = Modifier.rotate(-90f),
            icon = Res.drawable.ic_chevron,
            label = Res.string.sprite_editor_next_frame,
            onClick = { state.frameIndex = (state.frameIndex + 1).mod(frameCount) },
        )
    }
    Spacer(Modifier.weight(1f))
    IconActionButton(icon = Res.drawable.ic_zoom_out, label = Res.string.sprite_editor_zoom_out, onClick = state::zoomOut)
    Text(stringResource(Res.string.sprite_editor_zoom, state.zoom), style = MaterialTheme.typography.bodySmall)
    IconActionButton(icon = Res.drawable.ic_zoom_in, label = Res.string.sprite_editor_zoom_in, onClick = state::zoomIn)
    IconActionButton(icon = Res.drawable.ic_fit_screen, label = Res.string.sprite_editor_zoom_fit, onClick = onFit)
    MenuButton(
        label = stringResource(Res.string.sprite_editor_image),
        options = SpriteImageAction.entries,
        optionText = { stringResource(it.title) },
        onSelected = onImageAction,
    )
    if (frameCount > 1) {
        LabeledSwitch(
            label = stringResource(Res.string.sprite_editor_every_frame),
            checked = state.isEveryFrame,
            onCheckedChange = { state.isEveryFrame = it },
        )
    }
}

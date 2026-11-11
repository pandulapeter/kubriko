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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.sprite_editor_onion_skin
import kubriko.creator.generated.resources.sprite_editor_onion_skin_opacity
import kubriko.creator.generated.resources.sprite_editor_onion_skin_reach
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Turns onion skinning on and off, and while it is on, sets how many frames it shows each way and how strongly.
 */
@Composable
internal fun SpriteOnionSkinControls(
    state: SpriteEditorState,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.padding(horizontal = 12.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
) {
    LabeledSwitch(
        label = stringResource(Res.string.sprite_editor_onion_skin),
        checked = state.isOnionSkinOn,
        onCheckedChange = { state.isOnionSkinOn = it },
    )
    if (state.isOnionSkinOn) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuButton(
                label = pluralStringResource(Res.plurals.sprite_editor_onion_skin_reach, state.onionSkinReach, state.onionSkinReach),
                options = SpriteEditorState.ONION_SKIN_REACHES,
                optionText = { pluralStringResource(Res.plurals.sprite_editor_onion_skin_reach, it, it) },
                onSelected = { state.onionSkinReach = it },
            )
            val opacityLabel = stringResource(Res.string.sprite_editor_onion_skin_opacity)
            Slider(
                modifier = Modifier.width(96.dp).semantics { contentDescription = opacityLabel },
                value = state.onionSkinOpacity,
                onValueChange = { state.onionSkinOpacity = it },
                valueRange = SpriteEditorState.ONION_SKIN_OPACITIES,
            )
        }
    }
}

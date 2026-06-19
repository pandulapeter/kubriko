/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.metadataRow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.RadioButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.SceneEditorInteractionMode
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorSurface
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorTextInput
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorTextLabel
import com.pandulapeter.kubriko.types.SceneOffset
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.actor_count
import kubriko.tools.scene_editor.generated.resources.snap_x
import kubriko.tools.scene_editor.generated.resources.snap_y
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max
import kotlin.math.roundToInt


@Composable
internal fun MetadataRow(
    modifier: Modifier = Modifier,
    totalActorCount: Int,
    mouseSceneOffset: SceneOffset,
    snapMode: Pair<Int, Int>,
    onSnapModeChanged: (Pair<Int, Int>) -> Unit,
    interactionMode: SceneEditorInteractionMode,
    onInteractionModeSelected: (SceneEditorInteractionMode) -> Unit,
) = EditorSurface(
    modifier = modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorTextLabel(
            modifier = Modifier.width(92.dp),
            text = stringResource(Res.string.actor_count, totalActorCount),
        )
        EditorTextInput(
            modifier = Modifier.width(92.dp),
            title = stringResource(Res.string.snap_x),
            shouldUseHorizontalLayout = true,
            value = snapMode.first.toString(),
            onValueChanged = { newValue ->
                onSnapModeChanged(snapMode.copy(first = max(0, newValue.take(5).toIntOrNull() ?: 0)))
            },
        )
        EditorTextInput(
            modifier = Modifier.width(92.dp),
            title = stringResource(Res.string.snap_y),
            shouldUseHorizontalLayout = true,
            value = snapMode.second.toString(),
            onValueChanged = { newValue ->
                onSnapModeChanged(snapMode.copy(second = max(0, newValue.take(5).toIntOrNull() ?: 0)))
            },
        )
        Spacer(
            modifier = Modifier.weight(1f),
        )
        EditorTextLabel(
            text = "${mouseSceneOffset.x.raw.roundToInt()}:${mouseSceneOffset.y.raw.roundToInt()}",
        )
        VerticalDivider(
            modifier = Modifier.height(24.dp).padding(horizontal = 8.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SceneEditorInteractionMode.entries.forEach { mode ->
                InteractionModeRadioButton(
                    mode = mode,
                    isSelected = interactionMode == mode,
                    onSelected = { onInteractionModeSelected(mode) },
                )
            }
        }
    }
}

@Composable
private fun InteractionModeRadioButton(
    mode: SceneEditorInteractionMode,
    isSelected: Boolean,
    onSelected: () -> Unit,
) = Row(
    modifier = Modifier.clickable(onClick = onSelected),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp),
) {
    RadioButton(
        modifier = Modifier.scale(0.6f).size(16.dp),
        selected = isSelected,
        onClick = onSelected,
    )
    EditorTextLabel(
        text = stringResource(mode.label),
    )
}
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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorSurface
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorTextInput
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorTextLabel
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.math.max
import kotlin.math.roundToInt


@Composable
internal fun MetadataRow(
    modifier: Modifier = Modifier,
    totalActorCount: Int,
    mouseSceneOffset: SceneOffset,
    snapMode: Pair<Int, Int>,
    onSnapModeChanged: (Pair<Int, Int>) -> Unit,
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
            text = "Actors: $totalActorCount",
        )
        EditorTextInput(
            modifier = Modifier.width(92.dp),
            title = "Snap X:",
            shouldUseHorizontalLayout = true,
            value = snapMode.first.toString(),
            onValueChanged = { newValue ->
                onSnapModeChanged(snapMode.copy(first = max(0, newValue.take(5).toIntOrNull() ?: 0)))
            },
        )
        EditorTextInput(
            modifier = Modifier.width(92.dp),
            title = "Snap Y:",
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
    }
}
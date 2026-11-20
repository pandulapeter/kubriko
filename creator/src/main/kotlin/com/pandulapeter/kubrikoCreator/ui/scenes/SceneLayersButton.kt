/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneLayerFilter
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.RightClickMenu
import com.pandulapeter.kubrikoCreator.ui.components.rememberMenuAnchor
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_scene_layers
import kubriko.creator.generated.resources.action_show_every_layer
import kubriko.creator.generated.resources.ic_layers
import kubriko.creator.generated.resources.scene_layer
import kubriko.creator.generated.resources.scene_layer_hidden_and_locked_state
import kubriko.creator.generated.resources.scene_layer_hidden_state
import kubriko.creator.generated.resources.scene_layer_locked
import kubriko.creator.generated.resources.scene_layer_none
import kubriko.creator.generated.resources.scene_layer_shown
import org.jetbrains.compose.resources.stringResource

/**
 * The menu of the Scene's [layerIndices], each opening whether it is shown and whether it is locked, highlighted while
 * [filter] hides or locks any of them, which it then offers to undo at once.
 */
@Composable
internal fun SceneLayersButton(
    layerIndices: List<Int?>,
    filter: SceneLayerFilter,
    onFilterChange: (SceneLayerFilter) -> Unit,
    modifier: Modifier = Modifier,
) = Box(modifier) {
    val menu = rememberRightClickMenuState()
    val anchor = rememberMenuAnchor(menu)
    val isFiltering = filter != SceneLayerFilter()
    IconActionButton(
        modifier = anchor.modifier,
        icon = Res.drawable.ic_layers,
        label = Res.string.action_scene_layers,
        onClick = anchor::open,
        tint = if (isFiltering) MaterialTheme.colorScheme.primary else LocalContentColor.current,
    )
    val shown = stringResource(Res.string.scene_layer_shown)
    val locked = stringResource(Res.string.scene_layer_locked)
    val hiddenState = stringResource(Res.string.scene_layer_hidden_state)
    val hiddenAndLockedState = stringResource(Res.string.scene_layer_hidden_and_locked_state)
    val layerActions = layerIndices.map { layerIndex ->
        val isHidden = layerIndex != null && layerIndex in filter.hidden
        val isLocked = layerIndex in filter.locked
        ContextAction(
            label = layerIndex?.let { stringResource(Res.string.scene_layer, it) } ?: stringResource(Res.string.scene_layer_none),
            description = when {
                isHidden && isLocked -> hiddenAndLockedState
                isHidden -> hiddenState
                isLocked -> locked
                else -> null
            },
            children = listOfNotNull(
                layerIndex?.let { ContextAction(label = shown, isChecked = !isHidden) { onFilterChange(filter.withHidden(it, !isHidden)) } },
                ContextAction(label = locked, isChecked = isLocked) { onFilterChange(filter.withLocked(layerIndex, !isLocked)) },
            ),
        )
    }
    val reset = ContextAction(label = stringResource(Res.string.action_show_every_layer), isSeparated = true) { onFilterChange(SceneLayerFilter()) }
    RightClickMenu(state = menu, actions = if (isFiltering) layerActions + reset else layerActions)
}

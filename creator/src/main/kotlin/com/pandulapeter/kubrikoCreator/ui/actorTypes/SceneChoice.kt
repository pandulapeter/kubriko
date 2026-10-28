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

import androidx.compose.runtime.Composable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_scene_choose
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SceneChoice(
    scene: String,
    sceneNames: List<String>,
    onSelected: (String) -> Unit,
) = MenuButton(
    label = scene.ifEmpty { stringResource(Res.string.block_scene_choose) },
    options = sceneNames,
    optionText = { it },
    onSelected = onSelected,
)

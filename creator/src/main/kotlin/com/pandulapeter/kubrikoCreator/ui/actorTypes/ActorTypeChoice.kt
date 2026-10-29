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
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.event_collision_choose
import org.jetbrains.compose.resources.stringResource

/**
 * Which of the project's Actor types a Block is about.
 */
@Composable
internal fun ActorTypeChoice(
    actorType: String,
    actorTypeNames: List<String>,
    onSelected: (String) -> Unit,
) = MenuButton(
    label = actorType.ifEmpty { stringResource(Res.string.event_collision_choose) },
    options = actorTypeNames,
    optionText = { it },
    onSelected = onSelected,
)

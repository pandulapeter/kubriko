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
import com.pandulapeter.kubrikoCreator.actors.TIMER_NUMBERS
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.timer_name
import org.jetbrains.compose.resources.stringResource

/**
 * Which of the Actor's Timers an Event or a Block is about.
 */
@Composable
internal fun TimerChoice(
    timer: Int,
    onSelected: (Int) -> Unit,
) = MenuButton(
    label = stringResource(Res.string.timer_name, timer),
    options = TIMER_NUMBERS.toList(),
    optionText = { stringResource(Res.string.timer_name, it) },
    onSelected = onSelected,
)

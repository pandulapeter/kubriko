/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testInput.implementation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.testInput.implementation.GamepadSnapshot
import com.pandulapeter.kubriko.uiComponents.Panel
import kotlinx.collections.immutable.ImmutableList
import kubriko.examples.test_input.generated.resources.Res
import kubriko.examples.test_input.generated.resources.gamepad_buttons
import kubriko.examples.test_input.generated.resources.gamepad_buttons_none
import kubriko.examples.test_input.generated.resources.gamepad_header
import kubriko.examples.test_input.generated.resources.gamepad_none
import kubriko.examples.test_input.generated.resources.gamepad_none_web
import kubriko.examples.test_input.generated.resources.gamepad_sticks
import kubriko.examples.test_input.generated.resources.gamepad_triggers
import kubriko.examples.test_input.generated.resources.gamepad_unknown_name
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun Gamepads(
    modifier: Modifier = Modifier,
    gamepads: ImmutableList<GamepadSnapshot>,
    isRunningInBrowser: Boolean,
) = Panel(
    modifier = modifier,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (gamepads.isEmpty()) {
            GamepadText(text = stringResource(if (isRunningInBrowser) Res.string.gamepad_none_web else Res.string.gamepad_none))
        } else {
            gamepads.forEach { gamepad ->
                GamepadText(
                    text = stringResource(
                        Res.string.gamepad_header,
                        gamepad.index.toString(),
                        gamepad.name ?: stringResource(Res.string.gamepad_unknown_name),
                    ),
                )
                GamepadText(
                    text = stringResource(
                        Res.string.gamepad_sticks,
                        gamepad.leftStickX.readout,
                        gamepad.leftStickY.readout,
                        gamepad.rightStickX.readout,
                        gamepad.rightStickY.readout,
                    ),
                )
                GamepadText(
                    text = stringResource(
                        Res.string.gamepad_triggers,
                        gamepad.leftTrigger.readout,
                        gamepad.rightTrigger.readout,
                    ),
                )
                GamepadText(
                    text = if (gamepad.pressedButtons.isEmpty()) {
                        stringResource(Res.string.gamepad_buttons_none)
                    } else {
                        stringResource(Res.string.gamepad_buttons, gamepad.pressedButtons.joinToString { it.name })
                    },
                )
            }
        }
    }
}

@Composable
private fun GamepadText(
    text: String,
) = Text(
    style = MaterialTheme.typography.bodySmall,
    text = text,
)

private val Float.readout get() = ((this * 100).roundToInt() / 100f).toString()

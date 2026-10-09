/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testInput.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kubriko.examples.test_input.generated.resources.Res
import kubriko.examples.test_input.generated.resources.description
import kubriko.examples.test_input.generated.resources.gamepad_buttons
import kubriko.examples.test_input.generated.resources.gamepad_buttons_none
import kubriko.examples.test_input.generated.resources.gamepad_header
import kubriko.examples.test_input.generated.resources.gamepad_none
import kubriko.examples.test_input.generated.resources.gamepad_none_web
import kubriko.examples.test_input.generated.resources.gamepad_sticks
import kubriko.examples.test_input.generated.resources.gamepad_triggers
import kubriko.examples.test_input.generated.resources.gamepad_unknown_name

sealed interface InputTestStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areStringResourcesLoaded()

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.description).value.isNotBlank()
                && preloadedString(Res.string.gamepad_none).value.isNotBlank()
                && preloadedString(Res.string.gamepad_none_web).value.isNotBlank()
                && preloadedString(Res.string.gamepad_header).value.isNotBlank()
                && preloadedString(Res.string.gamepad_unknown_name).value.isNotBlank()
                && preloadedString(Res.string.gamepad_sticks).value.isNotBlank()
                && preloadedString(Res.string.gamepad_triggers).value.isNotBlank()
                && preloadedString(Res.string.gamepad_buttons).value.isNotBlank()
                && preloadedString(Res.string.gamepad_buttons_none).value.isNotBlank()
    }
}

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageBitmap
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kubriko.examples.demo_isometric_graphics.generated.resources.Res
import kubriko.examples.demo_isometric_graphics.generated.resources.description
import kubriko.examples.demo_isometric_graphics.generated.resources.map_01
import kubriko.examples.demo_isometric_graphics.generated.resources.texture_01

sealed interface IsometricGraphicsDemoStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areImageResourcesLoaded() && areStringResourcesLoaded()

        @Composable
        private fun areImageResourcesLoaded() = preloadedImageBitmap(Res.drawable.texture_01).value != null
                && preloadedImageBitmap(Res.drawable.map_01).value != null

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.description).value.isNotBlank()
    }
}

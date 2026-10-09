/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoContentShaders.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.shared.ui.areExpandControlsButtonResourcesLoaded
import com.pandulapeter.kubriko.shared.ui.areShadersNotSupportedMessageResourcesLoaded
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kubriko.examples.demo_content_shaders.generated.resources.Res
import kubriko.examples.demo_content_shaders.generated.resources.blur
import kubriko.examples.demo_content_shaders.generated.resources.chromatic_aberration
import kubriko.examples.demo_content_shaders.generated.resources.comic
import kubriko.examples.demo_content_shaders.generated.resources.description
import kubriko.examples.demo_content_shaders.generated.resources.ripple
import kubriko.examples.demo_content_shaders.generated.resources.smooth_pixelation
import kubriko.examples.demo_content_shaders.generated.resources.vignette

sealed interface ContentShadersDemoStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areExpandControlsButtonResourcesLoaded() && areShadersNotSupportedMessageResourcesLoaded() && areStringResourcesLoaded()

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.description).value.isNotBlank()
                && preloadedString(Res.string.chromatic_aberration).value.isNotBlank()
                && preloadedString(Res.string.ripple).value.isNotBlank()
                && preloadedString(Res.string.comic).value.isNotBlank()
                && preloadedString(Res.string.blur).value.isNotBlank()
                && preloadedString(Res.string.vignette).value.isNotBlank()
                && preloadedString(Res.string.smooth_pixelation).value.isNotBlank()
    }
}

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.shared.ui.areExpandControlsButtonResourcesLoaded
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kubriko.examples.demo_shader_animations.generated.resources.Res
import kubriko.examples.demo_shader_animations.generated.resources.alpha
import kubriko.examples.demo_shader_animations.generated.resources.clouds
import kubriko.examples.demo_shader_animations.generated.resources.color
import kubriko.examples.demo_shader_animations.generated.resources.cover
import kubriko.examples.demo_shader_animations.generated.resources.dark
import kubriko.examples.demo_shader_animations.generated.resources.description
import kubriko.examples.demo_shader_animations.generated.resources.ether
import kubriko.examples.demo_shader_animations.generated.resources.focus
import kubriko.examples.demo_shader_animations.generated.resources.frequency
import kubriko.examples.demo_shader_animations.generated.resources.gradient
import kubriko.examples.demo_shader_animations.generated.resources.hide_code
import kubriko.examples.demo_shader_animations.generated.resources.ic_code
import kubriko.examples.demo_shader_animations.generated.resources.light
import kubriko.examples.demo_shader_animations.generated.resources.noodle
import kubriko.examples.demo_shader_animations.generated.resources.scale
import kubriko.examples.demo_shader_animations.generated.resources.shaders_not_supported
import kubriko.examples.demo_shader_animations.generated.resources.show_code
import kubriko.examples.demo_shader_animations.generated.resources.sky_1
import kubriko.examples.demo_shader_animations.generated.resources.sky_2
import kubriko.examples.demo_shader_animations.generated.resources.speed
import kubriko.examples.demo_shader_animations.generated.resources.warp

sealed interface ShaderAnimationsDemoStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areExpandControlsButtonResourcesLoaded() && areIconResourcesLoaded() && areStringResourcesLoaded()

        @Composable
        private fun areIconResourcesLoaded() = preloadedImageVector(Res.drawable.ic_code).value != null

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.description).value.isNotBlank()
                && preloadedString(Res.string.shaders_not_supported).value.isNotBlank()
                && preloadedString(Res.string.show_code).value.isNotBlank()
                && preloadedString(Res.string.hide_code).value.isNotBlank()
                && preloadedString(Res.string.gradient).value.isNotBlank()
                && preloadedString(Res.string.ether).value.isNotBlank()
                && preloadedString(Res.string.noodle).value.isNotBlank()
                && preloadedString(Res.string.clouds).value.isNotBlank()
                && preloadedString(Res.string.warp).value.isNotBlank()
                && preloadedString(Res.string.scale).value.isNotBlank()
                && preloadedString(Res.string.speed).value.isNotBlank()
                && preloadedString(Res.string.dark).value.isNotBlank()
                && preloadedString(Res.string.light).value.isNotBlank()
                && preloadedString(Res.string.cover).value.isNotBlank()
                && preloadedString(Res.string.alpha).value.isNotBlank()
                && preloadedString(Res.string.sky_1).value.isNotBlank()
                && preloadedString(Res.string.sky_2).value.isNotBlank()
                && preloadedString(Res.string.focus).value.isNotBlank()
                && preloadedString(Res.string.frequency).value.isNotBlank()
                && preloadedString(Res.string.color).value.isNotBlank()
    }
}

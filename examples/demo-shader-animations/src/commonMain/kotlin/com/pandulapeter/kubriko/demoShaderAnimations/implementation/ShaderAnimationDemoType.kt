/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations.implementation

import kubriko.examples.demo_shader_animations.generated.resources.Res
import kubriko.examples.demo_shader_animations.generated.resources.clouds
import kubriko.examples.demo_shader_animations.generated.resources.ether
import kubriko.examples.demo_shader_animations.generated.resources.gradient
import kubriko.examples.demo_shader_animations.generated.resources.noodle
import kubriko.examples.demo_shader_animations.generated.resources.warp
import org.jetbrains.compose.resources.StringResource

internal enum class ShaderAnimationDemoType(
    val nameStringResource: StringResource,
) {
    CLOUD(nameStringResource = Res.string.clouds),
    ETHER(nameStringResource = Res.string.ether),
    GRADIENT(nameStringResource = Res.string.gradient),
    NOODLE(nameStringResource = Res.string.noodle),
    WARP(nameStringResource = Res.string.warp),
}
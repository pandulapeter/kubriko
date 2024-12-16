package com.pandulapeter.kubriko.demoShaderAnimations.implementation

import kubriko.examples.demo_shader_animations.generated.resources.Res
import kubriko.examples.demo_shader_animations.generated.resources.clouds
import kubriko.examples.demo_shader_animations.generated.resources.ether
import kubriko.examples.demo_shader_animations.generated.resources.noodle
import kubriko.examples.demo_shader_animations.generated.resources.gradient
import kubriko.examples.demo_shader_animations.generated.resources.warp
import org.jetbrains.compose.resources.StringResource

internal enum class ShaderAnimationDemoType(
    val nameStringResource: StringResource,
) {
    GRADIENT(nameStringResource = Res.string.gradient),
    ETHER(nameStringResource = Res.string.ether),
    NOODLE(nameStringResource = Res.string.noodle),
    CLOUD(nameStringResource = Res.string.clouds),
    WARP(nameStringResource = Res.string.warp),
}
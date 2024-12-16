package com.pandulapeter.kubriko.demoCustomShaders.implementation

import kubriko.examples.demo_custom_shaders.generated.resources.Res
import kubriko.examples.demo_custom_shaders.generated.resources.clouds
import kubriko.examples.demo_custom_shaders.generated.resources.ether
import kubriko.examples.demo_custom_shaders.generated.resources.fractal
import kubriko.examples.demo_custom_shaders.generated.resources.gradient
import kubriko.examples.demo_custom_shaders.generated.resources.warp
import org.jetbrains.compose.resources.StringResource

internal enum class CustomShaderDemoType(
    val nameStringResource: StringResource,
) {
    GRADIENT(nameStringResource = Res.string.gradient),
    FRACTAL(nameStringResource = Res.string.fractal),
    ETHER(nameStringResource = Res.string.ether),
    CLOUD(nameStringResource = Res.string.clouds),
    WARP(nameStringResource = Res.string.warp),
}
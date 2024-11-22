package com.pandulapeter.kubriko.demoCustomShaders.implementation

import kubriko.examples.demo_custom_shaders.generated.resources.Res
import kubriko.examples.demo_custom_shaders.generated.resources.clouds
import kubriko.examples.demo_custom_shaders.generated.resources.fractal
import kubriko.examples.demo_custom_shaders.generated.resources.gradient
import kubriko.examples.demo_custom_shaders.generated.resources.warp
import org.jetbrains.compose.resources.StringResource

internal enum class CustomShaderType(
    val nameStringResource: StringResource,
) {
    FRACTAL(nameStringResource = Res.string.fractal),
    CLOUDS(nameStringResource = Res.string.clouds),
    WARP(nameStringResource = Res.string.warp),
    GRADIENT(nameStringResource = Res.string.gradient),
}
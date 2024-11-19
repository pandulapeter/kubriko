package com.pandulapeter.kubrikoShowcase.implementation.shaders

import kubriko.examples.showcase.generated.resources.Res
import kubriko.examples.showcase.generated.resources.clouds
import kubriko.examples.showcase.generated.resources.fractal
import org.jetbrains.compose.resources.StringResource

internal enum class ShaderDemoType(
    val nameStringResource: StringResource,
) {
    CLOUDS(nameStringResource = Res.string.clouds),
    FRACTAL(nameStringResource = Res.string.fractal),
}
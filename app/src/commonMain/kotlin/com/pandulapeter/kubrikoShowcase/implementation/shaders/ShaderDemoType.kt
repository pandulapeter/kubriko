package com.pandulapeter.kubrikoShowcase.implementation.shaders

import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.clouds
import kubriko.app.generated.resources.fractal
import org.jetbrains.compose.resources.StringResource

internal enum class ShaderDemoType(
    val nameStringResource: StringResource,
) {
    FRACTAL(nameStringResource = Res.string.fractal),
    CLOUDS(nameStringResource = Res.string.clouds),
}
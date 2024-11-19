package com.pandulapeter.kubrikoShaderTest.implementation

import kubriko.examples.test_shader.generated.resources.Res
import kubriko.examples.test_shader.generated.resources.ic_dot
import org.jetbrains.compose.resources.DrawableResource

internal enum class DemoType(
    val label: String,
    val drawableResource: DrawableResource,
) {
    CLOUDS("Clouds", Res.drawable.ic_dot),
    FRACTAL("Fractal", Res.drawable.ic_dot),
}
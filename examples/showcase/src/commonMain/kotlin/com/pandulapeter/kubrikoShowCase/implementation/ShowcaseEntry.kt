package com.pandulapeter.kubrikoShowcase.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoShowcase.implementation.keyboardInput.KeyboardInputShowcase
import com.pandulapeter.kubrikoShowcase.implementation.performance.PerformanceShowcase
import com.pandulapeter.kubrikoShowcase.implementation.physics.PhysicsShowcase
import com.pandulapeter.kubrikoShowcase.implementation.pointerInput.PointerInputShowcase
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ShadersShowcase
import kubriko.examples.showcase.generated.resources.Res
import kubriko.examples.showcase.generated.resources.keyboard_input
import kubriko.examples.showcase.generated.resources.performance
import kubriko.examples.showcase.generated.resources.physics
import kubriko.examples.showcase.generated.resources.pointer_input
import kubriko.examples.showcase.generated.resources.shaders
import org.jetbrains.compose.resources.StringResource

internal enum class ShowcaseEntry(
    val titleStringResource: StringResource,
    val content: @Composable () -> Unit,
) {
    KEYBOARD_INPUT(
        titleStringResource = Res.string.keyboard_input,
        content = { KeyboardInputShowcase() },
    ),
    PERFORMANCE(
        titleStringResource = Res.string.performance,
        content = { PerformanceShowcase() },
    ),
    POINTER_INPUT(
        titleStringResource = Res.string.pointer_input,
        content = { PointerInputShowcase() },
    ),
    PHYSICS(
        titleStringResource = Res.string.physics,
        content = { PhysicsShowcase() },
    ),
    SHADERS(
        titleStringResource = Res.string.shaders,
        content = { ShadersShowcase() },
    ),
}
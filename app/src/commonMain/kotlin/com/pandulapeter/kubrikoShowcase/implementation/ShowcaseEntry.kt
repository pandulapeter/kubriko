package com.pandulapeter.kubrikoShowcase.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoShowcase.implementation.keyboardInput.KeyboardInputShowcase
import com.pandulapeter.kubrikoShowcase.implementation.performance.PerformanceShowcase
import com.pandulapeter.kubrikoShowcase.implementation.physics.PhysicsShowcase
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ShadersShowcase
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.keyboard_input
import kubriko.app.generated.resources.performance
import kubriko.app.generated.resources.physics
import kubriko.app.generated.resources.shaders
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

    //    POINTER_INPUT(
//        titleStringResource = Res.string.pointer_input,
//        content = { PointerInputShowcase() },
//    ),
    PHYSICS(
        titleStringResource = Res.string.physics,
        content = { PhysicsShowcase() },
    ),
    SHADERS(
        titleStringResource = Res.string.shaders,
        content = { ShadersShowcase() },
    ),
}
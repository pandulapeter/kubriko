package com.pandulapeter.kubrikoShowcase.implementation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoShowcase.implementation.physics.PhysicsShowcase
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ShadersShowcase
import kubriko.examples.showcase.generated.resources.Res
import kubriko.examples.showcase.generated.resources.keyboard_input
import kubriko.examples.showcase.generated.resources.performance_test
import kubriko.examples.showcase.generated.resources.physics
import kubriko.examples.showcase.generated.resources.pointer_input
import kubriko.examples.showcase.generated.resources.shaders
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal enum class ShowcaseEntry(
    val titleStringResource: StringResource,
    val content: @Composable () -> Unit = {
        Text(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 8.dp),
            text = stringResource(titleStringResource),
        )
    },
) {
    KEYBOARD_INPUT(
        titleStringResource = Res.string.keyboard_input,
    ),
    PERFORMANCE_TEST(
        titleStringResource = Res.string.performance_test,
    ),
    POINTER_INPUT(
        titleStringResource = Res.string.pointer_input,
    ),
    PHYSICS(
        titleStringResource = Res.string.physics,
        content = { PhysicsShowcase() }
    ),
    SHADERS(
        titleStringResource = Res.string.shaders,
        content = { ShadersShowcase() }
    ),
}
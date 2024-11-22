package com.pandulapeter.kubrikoShowcase.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoShowcase.implementation.keyboardInput.KeyboardInputShowcase
import com.pandulapeter.kubrikoShowcase.implementation.performance.PerformanceShowcase
import com.pandulapeter.kubrikoShowcase.implementation.physics.PhysicsShowcase
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ShadersShowcase
import com.pandulapeter.kubrikoWallbreaker.WallbreakerGame
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.demos
import kubriko.app.generated.resources.games
import kubriko.app.generated.resources.keyboard_input
import kubriko.app.generated.resources.keyboard_input_subtitle
import kubriko.app.generated.resources.performance
import kubriko.app.generated.resources.performance_subtitle
import kubriko.app.generated.resources.physics
import kubriko.app.generated.resources.physics_subtitle
import kubriko.app.generated.resources.shaders
import kubriko.app.generated.resources.shaders_subtitle
import kubriko.app.generated.resources.wallbreaker
import kubriko.app.generated.resources.wallbreaker_subtitle
import org.jetbrains.compose.resources.StringResource

internal enum class ShowcaseEntry(
    val type: ShowcaseEntryType,
    val titleStringResource: StringResource,
    val subtitleStringResource: StringResource,
    val content: @Composable () -> Unit,
) {
    KEYBOARD_INPUT(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.keyboard_input,
        subtitleStringResource = Res.string.keyboard_input_subtitle,
        content = { KeyboardInputShowcase() },
    ),
    PERFORMANCE(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.performance,
        subtitleStringResource = Res.string.performance_subtitle,
        content = { PerformanceShowcase() },
    ),
    PHYSICS(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.physics,
        subtitleStringResource = Res.string.physics_subtitle,
        content = { PhysicsShowcase() },
    ),
    SHADERS(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.shaders,
        subtitleStringResource = Res.string.shaders_subtitle,
        content = { ShadersShowcase() },
    ),
    WALLBREAKER(
        type = ShowcaseEntryType.GAME,
        titleStringResource = Res.string.wallbreaker,
        subtitleStringResource = Res.string.wallbreaker_subtitle,
        content = { WallbreakerGame() },
    ),
}

internal enum class ShowcaseEntryType(
    val titleStringResource: StringResource,
) {
    DEMO(
        titleStringResource = Res.string.demos,
    ),
    GAME(
        titleStringResource = Res.string.games,
    ),
}
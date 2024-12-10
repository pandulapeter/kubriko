package com.pandulapeter.kubrikoShowcase.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoBuiltInShaders.BuiltInShadersDemo
import com.pandulapeter.kubriko.demoCustomShaders.CustomShadersDemo
import com.pandulapeter.kubriko.demoInput.InputDemo
import com.pandulapeter.kubriko.demoPerformance.PerformanceDemo
import com.pandulapeter.kubriko.demoPhysics.PhysicsDemo
import com.pandulapeter.kubriko.gameWallbreaker.WallbreakerGame
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.demo_built_in_shaders
import kubriko.app.generated.resources.demo_built_in_shaders_subtitle
import kubriko.app.generated.resources.demo_custom_shaders
import kubriko.app.generated.resources.demo_custom_shaders_subtitle
import kubriko.app.generated.resources.demo_input
import kubriko.app.generated.resources.demo_input_subtitle
import kubriko.app.generated.resources.demo_performance
import kubriko.app.generated.resources.demo_performance_subtitle
import kubriko.app.generated.resources.demo_physics
import kubriko.app.generated.resources.demo_physics_subtitle
import kubriko.app.generated.resources.demos
import kubriko.app.generated.resources.game_wallbreaker
import kubriko.app.generated.resources.game_wallbreaker_subtitle
import kubriko.app.generated.resources.games
import org.jetbrains.compose.resources.StringResource

internal enum class ShowcaseEntry(
    val type: ShowcaseEntryType,
    val titleStringResource: StringResource,
    val subtitleStringResource: StringResource,
    val content: @Composable () -> Unit,
) {
    BUILT_IN_SHADERS(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.demo_built_in_shaders,
        subtitleStringResource = Res.string.demo_built_in_shaders_subtitle,
        content = { BuiltInShadersDemo() },
    ),
    CUSTOM_SHADERS(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.demo_custom_shaders,
        subtitleStringResource = Res.string.demo_custom_shaders_subtitle,
        content = { CustomShadersDemo() },
    ),
    INPUT(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.demo_input,
        subtitleStringResource = Res.string.demo_input_subtitle,
        content = { InputDemo() },
    ),
    PERFORMANCE(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.demo_performance,
        subtitleStringResource = Res.string.demo_performance_subtitle,
        content = { PerformanceDemo() },
    ),
    PHYSICS(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.demo_physics,
        subtitleStringResource = Res.string.demo_physics_subtitle,
        content = { PhysicsDemo() },
    ),
    WALLBREAKER(
        type = ShowcaseEntryType.GAME,
        titleStringResource = Res.string.game_wallbreaker,
        subtitleStringResource = Res.string.game_wallbreaker_subtitle,
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
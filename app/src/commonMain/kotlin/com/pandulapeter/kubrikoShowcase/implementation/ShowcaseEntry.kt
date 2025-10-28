/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.IsometricGraphicsDemoStateHolder
import com.pandulapeter.kubriko.demoParticles.implementation.ParticlesDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoStateHolder
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoStateHolder
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ShaderAnimationsDemoStateHolder
import com.pandulapeter.kubriko.testAudio.implementation.AudioTestStateHolder
import com.pandulapeter.kubriko.testCollision.implementation.CollisionTestStateHolder
import com.pandulapeter.kubriko.testInput.implementation.InputTestStateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ui.about.AboutScreenStateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ui.licenses.LicensesScreenStateHolder
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.demo_content_shaders
import kubriko.app.generated.resources.demo_content_shaders_subtitle
import kubriko.app.generated.resources.demo_isometric_graphics
import kubriko.app.generated.resources.demo_isometric_graphics_subtitle
import kubriko.app.generated.resources.demo_particles
import kubriko.app.generated.resources.demo_particles_subtitle
import kubriko.app.generated.resources.demo_performance
import kubriko.app.generated.resources.demo_performance_subtitle
import kubriko.app.generated.resources.demo_physics
import kubriko.app.generated.resources.demo_physics_subtitle
import kubriko.app.generated.resources.demo_shader_animations
import kubriko.app.generated.resources.demo_shader_animations_subtitle
import kubriko.app.generated.resources.demos
import kubriko.app.generated.resources.game_annoyed_penguins
import kubriko.app.generated.resources.game_annoyed_penguins_subtitle
import kubriko.app.generated.resources.game_blockys_journey
import kubriko.app.generated.resources.game_blockys_journey_subtitle
import kubriko.app.generated.resources.game_space_squadron
import kubriko.app.generated.resources.game_space_squadron_subtitle
import kubriko.app.generated.resources.game_wallbreaker
import kubriko.app.generated.resources.game_wallbreaker_subtitle
import kubriko.app.generated.resources.games
import kubriko.app.generated.resources.ic_demos
import kubriko.app.generated.resources.ic_games
import kubriko.app.generated.resources.ic_other
import kubriko.app.generated.resources.ic_tests
import kubriko.app.generated.resources.other
import kubriko.app.generated.resources.other_about
import kubriko.app.generated.resources.other_about_subtitle
import kubriko.app.generated.resources.other_licenses
import kubriko.app.generated.resources.other_licenses_subtitle
import kubriko.app.generated.resources.test_audio
import kubriko.app.generated.resources.test_audio_subtitle
import kubriko.app.generated.resources.test_collision
import kubriko.app.generated.resources.test_collision_subtitle
import kubriko.app.generated.resources.test_input
import kubriko.app.generated.resources.test_input_subtitle
import kubriko.app.generated.resources.tests
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

internal enum class ShowcaseEntry(
    val type: ShowcaseEntryType,
    val titleStringResource: StringResource,
    val subtitleStringResource: StringResource,
    val isProductionReady: Boolean = true,
    val areResourcesLoaded: @Composable () -> Boolean = { true },
) {
    // Games
    WALLBREAKER(
        type = ShowcaseEntryType.GAME,
        titleStringResource = Res.string.game_wallbreaker,
        subtitleStringResource = Res.string.game_wallbreaker_subtitle,
    ),
    SPACE_SQUADRON(
        type = ShowcaseEntryType.GAME,
        titleStringResource = Res.string.game_space_squadron,
        subtitleStringResource = Res.string.game_space_squadron_subtitle,
    ),
    ANNOYED_PENGUINS(
        type = ShowcaseEntryType.GAME,
        titleStringResource = Res.string.game_annoyed_penguins,
        subtitleStringResource = Res.string.game_annoyed_penguins_subtitle,
    ),
    BLOCKYS_JOURNEY(
        type = ShowcaseEntryType.GAME,
        titleStringResource = Res.string.game_blockys_journey,
        subtitleStringResource = Res.string.game_blockys_journey_subtitle,
        isProductionReady = false,
    ),

    // Demos
    CONTENT_SHADERS(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.demo_content_shaders,
        subtitleStringResource = Res.string.demo_content_shaders_subtitle,
        areResourcesLoaded = { ContentShadersDemoStateHolder.areResourcesLoaded() }
    ),
    ISOMETRIC_GRAPHICS(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.demo_isometric_graphics,
        subtitleStringResource = Res.string.demo_isometric_graphics_subtitle,
        areResourcesLoaded = { IsometricGraphicsDemoStateHolder.areResourcesLoaded() }
    ),
    PARTICLES(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.demo_particles,
        subtitleStringResource = Res.string.demo_particles_subtitle,
        areResourcesLoaded = { ParticlesDemoStateHolder.areResourcesLoaded() }
    ),
    PERFORMANCE(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.demo_performance,
        subtitleStringResource = Res.string.demo_performance_subtitle,
        areResourcesLoaded = { PerformanceDemoStateHolder.areResourcesLoaded() }
    ),
    PHYSICS(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.demo_physics,
        subtitleStringResource = Res.string.demo_physics_subtitle,
        areResourcesLoaded = { PhysicsDemoStateHolder.areResourcesLoaded() }
    ),
    SHADER_ANIMATIONS(
        type = ShowcaseEntryType.DEMO,
        titleStringResource = Res.string.demo_shader_animations,
        subtitleStringResource = Res.string.demo_shader_animations_subtitle,
        areResourcesLoaded = { ShaderAnimationsDemoStateHolder.areResourcesLoaded() }
    ),

    // Tests
    AUDIO(
        type = ShowcaseEntryType.TEST,
        titleStringResource = Res.string.test_audio,
        subtitleStringResource = Res.string.test_audio_subtitle,
        areResourcesLoaded = { AudioTestStateHolder.areResourcesLoaded() }
    ),
    COLLISION(
        type = ShowcaseEntryType.TEST,
        titleStringResource = Res.string.test_collision,
        subtitleStringResource = Res.string.test_collision_subtitle,
        areResourcesLoaded = { CollisionTestStateHolder.areResourcesLoaded() }
    ),
    INPUT(
        type = ShowcaseEntryType.TEST,
        titleStringResource = Res.string.test_input,
        subtitleStringResource = Res.string.test_input_subtitle,
        areResourcesLoaded = { InputTestStateHolder.areResourcesLoaded() }
    ),

    // Other
    LICENSES(
        type = ShowcaseEntryType.OTHER,
        titleStringResource = Res.string.other_licenses,
        subtitleStringResource = Res.string.other_licenses_subtitle,
        areResourcesLoaded = { LicensesScreenStateHolder.areResourcesLoaded() },
    ),
    ABOUT(
        type = ShowcaseEntryType.OTHER,
        titleStringResource = Res.string.other_about,
        subtitleStringResource = Res.string.other_about_subtitle,
        areResourcesLoaded = { AboutScreenStateHolder.areResourcesLoaded() },
    ),
}

internal enum class ShowcaseEntryType(
    val titleStringResource: StringResource,
    val iconDrawableResource: DrawableResource,
) {
    GAME(
        titleStringResource = Res.string.games,
        iconDrawableResource = Res.drawable.ic_games,
    ),
    DEMO(
        titleStringResource = Res.string.demos,
        iconDrawableResource = Res.drawable.ic_demos,
    ),
    TEST(
        titleStringResource = Res.string.tests,
        iconDrawableResource = Res.drawable.ic_tests,
    ),
    OTHER(
        titleStringResource = Res.string.other,
        iconDrawableResource = Res.drawable.ic_other,
    ),
}
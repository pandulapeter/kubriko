/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.pandulapeter.kubriko.demoContentShaders.ContentShadersDemo
import com.pandulapeter.kubriko.demoIsometricGraphics.IsometricGraphicsDemo
import com.pandulapeter.kubriko.demoParticles.ParticlesDemo
import com.pandulapeter.kubriko.demoPerformance.PerformanceDemo
import com.pandulapeter.kubriko.demoPhysics.PhysicsDemo
import com.pandulapeter.kubriko.demoShaderAnimations.ShaderAnimationsDemo
import com.pandulapeter.kubriko.gameAnnoyedPenguins.AnnoyedPenguinsGame
import com.pandulapeter.kubriko.gameBlockysJourney.BlockysJourneyGame
import com.pandulapeter.kubriko.gameSpaceSquadron.SpaceSquadronGame
import com.pandulapeter.kubriko.gameWallbreaker.WallbreakerGame
import com.pandulapeter.kubriko.testAudio.AudioTest
import com.pandulapeter.kubriko.testCollision.CollisionTest
import com.pandulapeter.kubriko.testInput.InputTest
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ui.about.AboutScreen
import com.pandulapeter.kubrikoShowcase.implementation.ui.licenses.LicensesScreen

@Composable
internal fun ShowcaseEntry.ExampleScreen(
    windowInsets: WindowInsets,
    isInFullscreenMode: Boolean?,
    onFullscreenModeToggled: () -> Unit,
    getSelectedShowcaseEntry: () -> ShowcaseEntry?,
) {
    when (this) {
        ShowcaseEntry.WALLBREAKER -> WallbreakerGame(
            stateHolder = wallbreakerGameStateHolder(),
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.SPACE_SQUADRON -> SpaceSquadronGame(
            stateHolder = spaceSquadronGameStateHolder(),
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.ANNOYED_PENGUINS -> AnnoyedPenguinsGame(
            stateHolder = annoyedPenguinsGameStateHolder(),
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.BLOCKYS_JOURNEY -> BlockysJourneyGame(
            stateHolder = blockysJourneyGameStateHolder(),
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.CONTENT_SHADERS -> ContentShadersDemo(
            stateHolder = contentShadersDemoStateHolder(),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.ISOMETRIC_GRAPHICS -> IsometricGraphicsDemo(
            stateHolder = isometricGraphicsDemoStateHolder(),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.PARTICLES -> ParticlesDemo(
            stateHolder = particlesDemoStateHolder(),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.PERFORMANCE -> PerformanceDemo(
            stateHolder = performanceDemoStateHolder(),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.PHYSICS -> PhysicsDemo(
            stateHolder = physicsDemoStateHolder(),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.SHADER_ANIMATIONS -> ShaderAnimationsDemo(
            stateHolder = shaderAnimationsDemoStateHolder(),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.AUDIO -> AudioTest(
            stateHolder = audioTestStateHolder(),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.COLLISION -> CollisionTest(
            stateHolder = collisionTestStateHolder(),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.INPUT -> InputTest(
            stateHolder = inputTestStateHolder(),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.LICENSES -> LicensesScreen(
            stateHolder = licensesScreenStateHolder(),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.ABOUT -> AboutScreen(
            stateHolder = aboutScreenStateHolder(),
            windowInsets = windowInsets,
        )
    }
    DisposableEffect(type) {
        onDispose {
            if (getSelectedShowcaseEntry() != this@ExampleScreen) {
                disposeStateHolder()
            }
        }
    }
}
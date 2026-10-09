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
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoIsometricGraphics.IsometricGraphicsDemo
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.IsometricGraphicsDemoStateHolder
import com.pandulapeter.kubriko.demoParticles.ParticlesDemo
import com.pandulapeter.kubriko.demoParticles.implementation.ParticlesDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.PerformanceDemo
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoStateHolder
import com.pandulapeter.kubriko.demoPhysics.PhysicsDemo
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoStateHolder
import com.pandulapeter.kubriko.demoShaderAnimations.ShaderAnimationsDemo
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ShaderAnimationsDemoStateHolder
import com.pandulapeter.kubriko.gameAnnoyedPenguins.AnnoyedPenguinsGame
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.AnnoyedPenguinsGameStateHolder
import com.pandulapeter.kubriko.gameBlockysJourney.BlockysJourneyGame
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.BlockysJourneyGameStateHolder
import com.pandulapeter.kubriko.gameSpaceSquadron.SpaceSquadronGame
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.SpaceSquadronGameStateHolder
import com.pandulapeter.kubriko.gameWallbreaker.WallbreakerGame
import com.pandulapeter.kubriko.gameWallbreaker.implementation.WallbreakerGameStateHolder
import com.pandulapeter.kubriko.testAudio.AudioTest
import com.pandulapeter.kubriko.testAudio.implementation.AudioTestStateHolder
import com.pandulapeter.kubriko.testCollision.CollisionTest
import com.pandulapeter.kubriko.testCollision.implementation.CollisionTestStateHolder
import com.pandulapeter.kubriko.testInput.InputTest
import com.pandulapeter.kubriko.testInput.implementation.InputTestStateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseSession
import com.pandulapeter.kubrikoShowcase.implementation.ui.about.AboutScreen
import com.pandulapeter.kubrikoShowcase.implementation.ui.about.AboutScreenStateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ui.licenses.LicensesScreen
import com.pandulapeter.kubrikoShowcase.implementation.ui.licenses.LicensesScreenStateHolder

@Composable
internal fun ShowcaseEntry.ExampleScreen(
    windowInsets: WindowInsets,
    isInFullscreenMode: Boolean?,
    onFullscreenModeToggled: () -> Unit,
    session: ShowcaseSession,
) {
    val stateHolder = session.holderFor(this)
    when (this) {
        ShowcaseEntry.WALLBREAKER -> WallbreakerGame(
            stateHolder = stateHolder as WallbreakerGameStateHolder,
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.SPACE_SQUADRON -> SpaceSquadronGame(
            stateHolder = stateHolder as SpaceSquadronGameStateHolder,
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.ANNOYED_PENGUINS -> AnnoyedPenguinsGame(
            stateHolder = stateHolder as AnnoyedPenguinsGameStateHolder,
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.BLOCKYS_JOURNEY -> BlockysJourneyGame(
            stateHolder = stateHolder as BlockysJourneyGameStateHolder,
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.CONTENT_SHADERS -> ContentShadersDemo(
            stateHolder = stateHolder as ContentShadersDemoStateHolder,
            windowInsets = windowInsets,
        )

        ShowcaseEntry.ISOMETRIC_GRAPHICS -> IsometricGraphicsDemo(
            stateHolder = stateHolder as IsometricGraphicsDemoStateHolder,
            windowInsets = windowInsets,
        )

        ShowcaseEntry.PARTICLES -> ParticlesDemo(
            stateHolder = stateHolder as ParticlesDemoStateHolder,
            windowInsets = windowInsets,
        )

        ShowcaseEntry.PERFORMANCE -> PerformanceDemo(
            stateHolder = stateHolder as PerformanceDemoStateHolder,
            windowInsets = windowInsets,
        )

        ShowcaseEntry.PHYSICS -> PhysicsDemo(
            stateHolder = stateHolder as PhysicsDemoStateHolder,
            windowInsets = windowInsets,
        )

        ShowcaseEntry.SHADER_ANIMATIONS -> ShaderAnimationsDemo(
            stateHolder = stateHolder as ShaderAnimationsDemoStateHolder,
            windowInsets = windowInsets,
        )

        ShowcaseEntry.AUDIO -> AudioTest(
            stateHolder = stateHolder as AudioTestStateHolder,
            windowInsets = windowInsets,
        )

        ShowcaseEntry.COLLISION -> CollisionTest(
            stateHolder = stateHolder as CollisionTestStateHolder,
            windowInsets = windowInsets,
        )

        ShowcaseEntry.INPUT -> InputTest(
            stateHolder = stateHolder as InputTestStateHolder,
            windowInsets = windowInsets,
        )

        ShowcaseEntry.LICENSES -> LicensesScreen(
            stateHolder = stateHolder as LicensesScreenStateHolder,
            windowInsets = windowInsets,
        )

        ShowcaseEntry.ABOUT -> AboutScreen(
            stateHolder = stateHolder as AboutScreenStateHolder,
            windowInsets = windowInsets,
        )
    }
    DisposableEffect(type) {
        onDispose {
            if (!session.isSelected(this@ExampleScreen)) {
                session.release(this@ExampleScreen)
            }
        }
    }
}
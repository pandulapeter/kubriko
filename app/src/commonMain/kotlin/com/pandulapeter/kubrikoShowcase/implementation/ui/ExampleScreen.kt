/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.pandulapeter.kubriko.demoContentShaders.ContentShadersDemo
import com.pandulapeter.kubriko.demoContentShaders.createContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoInput.InputTest
import com.pandulapeter.kubriko.demoInput.createInputTestStateHolder
import com.pandulapeter.kubriko.demoInput.implementation.InputTestStateHolder
import com.pandulapeter.kubriko.demoParticles.ParticlesDemo
import com.pandulapeter.kubriko.demoParticles.createParticlesDemoStateHolder
import com.pandulapeter.kubriko.demoParticles.implementation.ParticlesDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.PerformanceTest
import com.pandulapeter.kubriko.demoPerformance.createPerformanceTestStateHolder
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceTestStateHolder
import com.pandulapeter.kubriko.demoPhysics.PhysicsDemo
import com.pandulapeter.kubriko.demoPhysics.createPhysicsDemoStateHolder
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoStateHolder
import com.pandulapeter.kubriko.demoShaderAnimations.ShaderAnimationsDemo
import com.pandulapeter.kubriko.demoShaderAnimations.createShaderAnimationsDemoStateHolder
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ShaderAnimationsDemoStateHolder
import com.pandulapeter.kubriko.gameAnnoyedPenguins.AnnoyedPenguinsGame
import com.pandulapeter.kubriko.gameAnnoyedPenguins.createAnnoyedPenguinsGameStateHolder
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.AnnoyedPenguinsGameStateHolder
import com.pandulapeter.kubriko.gameSpaceSquadron.SpaceSquadronGame
import com.pandulapeter.kubriko.gameSpaceSquadron.createSpaceSquadronGameStateHolder
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.SpaceSquadronGameStateHolder
import com.pandulapeter.kubriko.gameWallbreaker.WallbreakerGame
import com.pandulapeter.kubriko.gameWallbreaker.createWallbreakerGameStateHolder
import com.pandulapeter.kubriko.gameWallbreaker.implementation.WallbreakerGameStateHolder
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.testAudio.AudioTest
import com.pandulapeter.kubriko.testAudio.createAudioTestStateHolder
import com.pandulapeter.kubriko.testAudio.implementation.AudioTestStateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry

private val stateHolders = mutableStateOf(emptyList<StateHolder>())

@Composable
internal fun ShowcaseEntry.ExampleScreen(
    windowInsets: WindowInsets,
    isInFullscreenMode: Boolean,
    onFullscreenModeToggled: () -> Unit,
    getSelectedShowcaseEntry: () -> ShowcaseEntry?,
) {
    when (this) {
        ShowcaseEntry.WALLBREAKER -> WallbreakerGame(
            stateHolder = getOrCreateState(stateHolders, ::createWallbreakerGameStateHolder),
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.SPACE_SQUADRON -> SpaceSquadronGame(
            stateHolder = getOrCreateState(stateHolders, ::createSpaceSquadronGameStateHolder),
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.ANNOYED_PENGUINS -> AnnoyedPenguinsGame(
            stateHolder = getOrCreateState(stateHolders, ::createAnnoyedPenguinsGameStateHolder),
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.AUDIO -> AudioTest(
            stateHolder = getOrCreateState(stateHolders, ::createAudioTestStateHolder),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.CONTENT_SHADERS -> ContentShadersDemo(
            stateHolder = getOrCreateState(stateHolders, ::createContentShadersDemoStateHolder),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.INPUT -> InputTest(
            stateHolder = getOrCreateState(stateHolders, ::createInputTestStateHolder),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.PARTICLES -> ParticlesDemo(
            stateHolder = getOrCreateState(stateHolders, ::createParticlesDemoStateHolder),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.PERFORMANCE -> PerformanceTest(
            stateHolder = getOrCreateState(stateHolders, ::createPerformanceTestStateHolder),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.PHYSICS -> PhysicsDemo(
            stateHolder = getOrCreateState(stateHolders, ::createPhysicsDemoStateHolder),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.SHADER_ANIMATIONS -> ShaderAnimationsDemo(
            stateHolder = getOrCreateState(stateHolders, ::createShaderAnimationsDemoStateHolder),
            windowInsets = windowInsets,
        )
    }
    DisposableEffect(type) {
        onDispose {
            if (getSelectedShowcaseEntry() != this@ExampleScreen) {
                stateHolderType.let { type ->
                    stateHolders.value.filter { type.isInstance(it) }.forEach { it.dispose() }
                    stateHolders.value = stateHolders.value.filterNot { type.isInstance(it) }
                }
            }
        }
    }
}

internal fun ShowcaseEntry.getStateHolder() = when (this) {
    ShowcaseEntry.WALLBREAKER -> getOrCreateState(stateHolders, ::createWallbreakerGameStateHolder)
    ShowcaseEntry.SPACE_SQUADRON -> getOrCreateState(stateHolders, ::createSpaceSquadronGameStateHolder)
    ShowcaseEntry.ANNOYED_PENGUINS -> getOrCreateState(stateHolders, ::createAnnoyedPenguinsGameStateHolder)
    ShowcaseEntry.AUDIO -> getOrCreateState(stateHolders, ::createAudioTestStateHolder)
    ShowcaseEntry.CONTENT_SHADERS -> getOrCreateState(stateHolders, ::createContentShadersDemoStateHolder)
    ShowcaseEntry.INPUT -> getOrCreateState(stateHolders, ::createInputTestStateHolder)
    ShowcaseEntry.PARTICLES -> getOrCreateState(stateHolders, ::createParticlesDemoStateHolder)
    ShowcaseEntry.PERFORMANCE -> getOrCreateState(stateHolders, ::createPerformanceTestStateHolder)
    ShowcaseEntry.PHYSICS -> getOrCreateState(stateHolders, ::createPhysicsDemoStateHolder)
    ShowcaseEntry.SHADER_ANIMATIONS -> getOrCreateState(stateHolders, ::createShaderAnimationsDemoStateHolder)
}

private inline fun <reified T : StateHolder> getOrCreateState(
    stateHolders: MutableState<List<StateHolder>>,
    creator: () -> T
): T = stateHolders.value.filterIsInstance<T>().firstOrNull() ?: creator().also { stateHolders.value += it }

private val ShowcaseEntry.stateHolderType
    get() = when (this) {
        ShowcaseEntry.CONTENT_SHADERS -> ContentShadersDemoStateHolder::class
        ShowcaseEntry.SHADER_ANIMATIONS -> ShaderAnimationsDemoStateHolder::class
        ShowcaseEntry.AUDIO -> AudioTestStateHolder::class
        ShowcaseEntry.INPUT -> InputTestStateHolder::class
        ShowcaseEntry.PARTICLES -> ParticlesDemoStateHolder::class
        ShowcaseEntry.PERFORMANCE -> PerformanceTestStateHolder::class
        ShowcaseEntry.PHYSICS -> PhysicsDemoStateHolder::class
        ShowcaseEntry.SPACE_SQUADRON -> SpaceSquadronGameStateHolder::class
        ShowcaseEntry.WALLBREAKER -> WallbreakerGameStateHolder::class
        ShowcaseEntry.ANNOYED_PENGUINS -> AnnoyedPenguinsGameStateHolder::class
    }
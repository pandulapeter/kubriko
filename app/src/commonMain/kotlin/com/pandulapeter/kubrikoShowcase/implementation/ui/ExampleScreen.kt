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
import com.pandulapeter.kubriko.demoParticles.ParticlesDemo
import com.pandulapeter.kubriko.demoParticles.createParticlesDemoStateHolder
import com.pandulapeter.kubriko.demoParticles.implementation.ParticlesDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.PerformanceDemo
import com.pandulapeter.kubriko.demoPerformance.createPerformanceDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoStateHolder
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
import com.pandulapeter.kubriko.testInput.InputTest
import com.pandulapeter.kubriko.testInput.createInputTestStateHolder
import com.pandulapeter.kubriko.testInput.implementation.InputTestStateHolder
import com.pandulapeter.kubrikoShowcase.BuildConfig
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ui.about.AboutScreen
import com.pandulapeter.kubrikoShowcase.implementation.ui.about.AboutScreenStateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ui.about.createAboutScreenStateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ui.licenses.LicensesScreen
import com.pandulapeter.kubrikoShowcase.implementation.ui.licenses.LicensesScreenStateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ui.licenses.createLicensesScreenStateHolder

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
            stateHolder = getOrCreateState(stateHolders) {
                createWallbreakerGameStateHolder(
                    webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
                )
            },
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.SPACE_SQUADRON -> SpaceSquadronGame(
            stateHolder = getOrCreateState(stateHolders) {
                createSpaceSquadronGameStateHolder(
                    webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
                )
            },
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.ANNOYED_PENGUINS -> AnnoyedPenguinsGame(
            stateHolder = getOrCreateState(stateHolders) {
                createAnnoyedPenguinsGameStateHolder(
                    webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
                    isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
                )
            },
            windowInsets = windowInsets,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.CONTENT_SHADERS -> ContentShadersDemo(
            stateHolder = getOrCreateState(stateHolders, ::createContentShadersDemoStateHolder),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.PARTICLES -> ParticlesDemo(
            stateHolder = getOrCreateState(stateHolders, ::createParticlesDemoStateHolder),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.PERFORMANCE -> PerformanceDemo(
            stateHolder = getOrCreateState(stateHolders) {
                createPerformanceDemoStateHolder(
                    isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
                )
            },
            windowInsets = windowInsets,
        )

        ShowcaseEntry.PHYSICS -> PhysicsDemo(
            stateHolder = getOrCreateState(stateHolders) {
                createPhysicsDemoStateHolder(
                    isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
                )
            },
            windowInsets = windowInsets,
        )

        ShowcaseEntry.SHADER_ANIMATIONS -> ShaderAnimationsDemo(
            stateHolder = getOrCreateState(stateHolders, ::createShaderAnimationsDemoStateHolder),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.AUDIO -> AudioTest(
            stateHolder = getOrCreateState(stateHolders) {
                createAudioTestStateHolder(
                    webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
                )
            },
            windowInsets = windowInsets,
        )

        ShowcaseEntry.INPUT -> InputTest(
            stateHolder = getOrCreateState(stateHolders, ::createInputTestStateHolder),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.ABOUT -> AboutScreen(
            stateHolder = getOrCreateState(stateHolders, ::createAboutScreenStateHolder),
            windowInsets = windowInsets,
        )

        ShowcaseEntry.LICENSES -> LicensesScreen(
            stateHolder = getOrCreateState(stateHolders, ::createLicensesScreenStateHolder),
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
    ShowcaseEntry.WALLBREAKER -> getOrCreateState(stateHolders) {
        createWallbreakerGameStateHolder(
            webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        )
    }

    ShowcaseEntry.SPACE_SQUADRON -> getOrCreateState(stateHolders) {
        createSpaceSquadronGameStateHolder(
            webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        )
    }

    ShowcaseEntry.ANNOYED_PENGUINS -> getOrCreateState(stateHolders) {
        createAnnoyedPenguinsGameStateHolder(
            webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
            isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
        )
    }

    ShowcaseEntry.CONTENT_SHADERS -> getOrCreateState(stateHolders, ::createContentShadersDemoStateHolder)

    ShowcaseEntry.PARTICLES -> getOrCreateState(stateHolders, ::createParticlesDemoStateHolder)

    ShowcaseEntry.PERFORMANCE -> getOrCreateState(stateHolders) {
        createPerformanceDemoStateHolder(
            isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
        )
    }

    ShowcaseEntry.PHYSICS -> getOrCreateState(stateHolders) {
        createPhysicsDemoStateHolder(
            isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
        )
    }

    ShowcaseEntry.SHADER_ANIMATIONS -> getOrCreateState(stateHolders, ::createShaderAnimationsDemoStateHolder)

    ShowcaseEntry.AUDIO -> getOrCreateState(stateHolders) {
        createAudioTestStateHolder(
            webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        )
    }

    ShowcaseEntry.INPUT -> getOrCreateState(stateHolders, ::createInputTestStateHolder)

    ShowcaseEntry.ABOUT -> getOrCreateState(stateHolders, ::createAboutScreenStateHolder)

    ShowcaseEntry.LICENSES -> getOrCreateState(stateHolders, ::createLicensesScreenStateHolder)
}

private inline fun <reified T : StateHolder> getOrCreateState(
    stateHolders: MutableState<List<StateHolder>>,
    creator: () -> T
): T = stateHolders.value.filterIsInstance<T>().firstOrNull() ?: creator().also { stateHolders.value += it }

private val ShowcaseEntry.stateHolderType
    get() = when (this) {
        ShowcaseEntry.WALLBREAKER -> WallbreakerGameStateHolder::class
        ShowcaseEntry.SPACE_SQUADRON -> SpaceSquadronGameStateHolder::class
        ShowcaseEntry.ANNOYED_PENGUINS -> AnnoyedPenguinsGameStateHolder::class
        ShowcaseEntry.CONTENT_SHADERS -> ContentShadersDemoStateHolder::class
        ShowcaseEntry.PARTICLES -> ParticlesDemoStateHolder::class
        ShowcaseEntry.PERFORMANCE -> PerformanceDemoStateHolder::class
        ShowcaseEntry.PHYSICS -> PhysicsDemoStateHolder::class
        ShowcaseEntry.SHADER_ANIMATIONS -> ShaderAnimationsDemoStateHolder::class
        ShowcaseEntry.AUDIO -> AudioTestStateHolder::class
        ShowcaseEntry.INPUT -> InputTestStateHolder::class
        ShowcaseEntry.ABOUT -> AboutScreenStateHolder::class
        ShowcaseEntry.LICENSES -> LicensesScreenStateHolder::class
    }
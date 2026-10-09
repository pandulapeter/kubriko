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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.pandulapeter.kubriko.demoContentShaders.createContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoIsometricGraphics.createIsometricGraphicsDemoStateHolder
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.IsometricGraphicsDemoStateHolder
import com.pandulapeter.kubriko.demoParticles.createParticlesDemoStateHolder
import com.pandulapeter.kubriko.demoParticles.implementation.ParticlesDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.createPerformanceDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoStateHolder
import com.pandulapeter.kubriko.demoPhysics.createPhysicsDemoStateHolder
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoStateHolder
import com.pandulapeter.kubriko.demoShaderAnimations.createShaderAnimationsDemoStateHolder
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ShaderAnimationsDemoStateHolder
import com.pandulapeter.kubriko.gameAnnoyedPenguins.createAnnoyedPenguinsGameStateHolder
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.AnnoyedPenguinsGameStateHolder
import com.pandulapeter.kubriko.gameBlockysJourney.createBlockysJourneyGameStateHolder
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.BlockysJourneyGameStateHolder
import com.pandulapeter.kubriko.gameSpaceSquadron.createSpaceSquadronGameStateHolder
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.SpaceSquadronGameStateHolder
import com.pandulapeter.kubriko.gameWallbreaker.createWallbreakerGameStateHolder
import com.pandulapeter.kubriko.gameWallbreaker.implementation.WallbreakerGameStateHolder
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.testAudio.createAudioTestStateHolder
import com.pandulapeter.kubriko.testAudio.implementation.AudioTestStateHolder
import com.pandulapeter.kubriko.testCollision.createCollisionTestStateHolder
import com.pandulapeter.kubriko.testCollision.implementation.CollisionTestStateHolder
import com.pandulapeter.kubriko.testInput.createInputTestStateHolder
import com.pandulapeter.kubriko.testInput.implementation.InputTestStateHolder
import com.pandulapeter.kubrikoShowcase.BuildConfig
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ui.about.AboutScreenStateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ui.about.createAboutScreenStateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ui.licenses.LicensesScreenStateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ui.licenses.createLicensesScreenStateHolder

private val stateHolders = mutableStateOf(emptyList<StateHolder>())

internal fun wallbreakerGameStateHolder() = getOrCreateState(stateHolders) {
    createWallbreakerGameStateHolder(
        webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}

internal fun spaceSquadronGameStateHolder() = getOrCreateState(stateHolders) {
    createSpaceSquadronGameStateHolder(
        webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}

internal fun annoyedPenguinsGameStateHolder() = getOrCreateState(stateHolders) {
    createAnnoyedPenguinsGameStateHolder(
        webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}

internal fun blockysJourneyGameStateHolder() = getOrCreateState(stateHolders) {
    createBlockysJourneyGameStateHolder(
        webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}

internal fun contentShadersDemoStateHolder() = getOrCreateState(stateHolders) {
    createContentShadersDemoStateHolder(
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}

internal fun isometricGraphicsDemoStateHolder() = getOrCreateState(stateHolders) {
    createIsometricGraphicsDemoStateHolder(
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}

internal fun particlesDemoStateHolder() = getOrCreateState(stateHolders) {
    createParticlesDemoStateHolder(
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}

internal fun performanceDemoStateHolder() = getOrCreateState(stateHolders) {
    createPerformanceDemoStateHolder(
        isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}

internal fun physicsDemoStateHolder() = getOrCreateState(stateHolders) {
    createPhysicsDemoStateHolder(
        isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}

internal fun shaderAnimationsDemoStateHolder() = getOrCreateState(stateHolders) {
    createShaderAnimationsDemoStateHolder(
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}

internal fun audioTestStateHolder() = getOrCreateState(stateHolders) {
    createAudioTestStateHolder(
        webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}

internal fun collisionTestStateHolder() = getOrCreateState(stateHolders) {
    createCollisionTestStateHolder(
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}

internal fun inputTestStateHolder() = getOrCreateState(stateHolders) {
    createInputTestStateHolder(
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}

internal fun licensesScreenStateHolder() = getOrCreateState(stateHolders, ::createLicensesScreenStateHolder)

internal fun aboutScreenStateHolder() = getOrCreateState(stateHolders, ::createAboutScreenStateHolder)

internal fun ShowcaseEntry.getStateHolder() = when (this) {
    ShowcaseEntry.WALLBREAKER -> wallbreakerGameStateHolder()
    ShowcaseEntry.SPACE_SQUADRON -> spaceSquadronGameStateHolder()
    ShowcaseEntry.ANNOYED_PENGUINS -> annoyedPenguinsGameStateHolder()
    ShowcaseEntry.BLOCKYS_JOURNEY -> blockysJourneyGameStateHolder()
    ShowcaseEntry.CONTENT_SHADERS -> contentShadersDemoStateHolder()
    ShowcaseEntry.ISOMETRIC_GRAPHICS -> isometricGraphicsDemoStateHolder()
    ShowcaseEntry.PARTICLES -> particlesDemoStateHolder()
    ShowcaseEntry.PERFORMANCE -> performanceDemoStateHolder()
    ShowcaseEntry.PHYSICS -> physicsDemoStateHolder()
    ShowcaseEntry.SHADER_ANIMATIONS -> shaderAnimationsDemoStateHolder()
    ShowcaseEntry.AUDIO -> audioTestStateHolder()
    ShowcaseEntry.COLLISION -> collisionTestStateHolder()
    ShowcaseEntry.INPUT -> inputTestStateHolder()
    ShowcaseEntry.LICENSES -> licensesScreenStateHolder()
    ShowcaseEntry.ABOUT -> aboutScreenStateHolder()
}

internal fun ShowcaseEntry.disposeStateHolder() {
    stateHolderType.let { type ->
        stateHolders.value.filter { type.isInstance(it) }.forEach { it.dispose() }
        stateHolders.value = stateHolders.value.filterNot { type.isInstance(it) }
    }
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
        ShowcaseEntry.BLOCKYS_JOURNEY -> BlockysJourneyGameStateHolder::class
        ShowcaseEntry.CONTENT_SHADERS -> ContentShadersDemoStateHolder::class
        ShowcaseEntry.ISOMETRIC_GRAPHICS -> IsometricGraphicsDemoStateHolder::class
        ShowcaseEntry.PARTICLES -> ParticlesDemoStateHolder::class
        ShowcaseEntry.PERFORMANCE -> PerformanceDemoStateHolder::class
        ShowcaseEntry.PHYSICS -> PhysicsDemoStateHolder::class
        ShowcaseEntry.SHADER_ANIMATIONS -> ShaderAnimationsDemoStateHolder::class
        ShowcaseEntry.AUDIO -> AudioTestStateHolder::class
        ShowcaseEntry.COLLISION -> CollisionTestStateHolder::class
        ShowcaseEntry.INPUT -> InputTestStateHolder::class
        ShowcaseEntry.LICENSES -> LicensesScreenStateHolder::class
        ShowcaseEntry.ABOUT -> AboutScreenStateHolder::class
    }

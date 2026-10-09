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

import com.pandulapeter.kubriko.demoContentShaders.createContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoIsometricGraphics.createIsometricGraphicsDemoStateHolder
import com.pandulapeter.kubriko.demoParticles.createParticlesDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.createPerformanceDemoStateHolder
import com.pandulapeter.kubriko.demoPhysics.createPhysicsDemoStateHolder
import com.pandulapeter.kubriko.demoShaderAnimations.createShaderAnimationsDemoStateHolder
import com.pandulapeter.kubriko.gameAnnoyedPenguins.createAnnoyedPenguinsGameStateHolder
import com.pandulapeter.kubriko.gameBlockysJourney.createBlockysJourneyGameStateHolder
import com.pandulapeter.kubriko.gameSpaceSquadron.createSpaceSquadronGameStateHolder
import com.pandulapeter.kubriko.gameWallbreaker.createWallbreakerGameStateHolder
import com.pandulapeter.kubriko.shared.SceneEditorConnection
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.testAudio.createAudioTestStateHolder
import com.pandulapeter.kubriko.testCollision.createCollisionTestStateHolder
import com.pandulapeter.kubriko.testInput.createInputTestStateHolder
import com.pandulapeter.kubrikoShowcase.BuildConfig
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ui.about.createAboutScreenStateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ui.licenses.createLicensesScreenStateHolder

/**
 * Creates the [StateHolder] of the example or screen [entry] opens. The examples with a scene editor receive
 * [sceneEditorConnection] where the platform has scene editor windows.
 */
internal fun createShowcaseStateHolder(
    entry: ShowcaseEntry,
    sceneEditorConnection: SceneEditorConnection,
): StateHolder = when (entry) {
    ShowcaseEntry.WALLBREAKER -> createWallbreakerGameStateHolder(
        webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )

    ShowcaseEntry.SPACE_SQUADRON -> createSpaceSquadronGameStateHolder(
        webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )

    ShowcaseEntry.ANNOYED_PENGUINS -> createAnnoyedPenguinsGameStateHolder(
        webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
        sceneEditorConnection = sceneEditorConnection.takeIf { isSceneEditorWindowAvailable },
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )

    ShowcaseEntry.BLOCKYS_JOURNEY -> createBlockysJourneyGameStateHolder(
        webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
        sceneEditorConnection = sceneEditorConnection.takeIf { isSceneEditorWindowAvailable },
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )

    ShowcaseEntry.CONTENT_SHADERS -> createContentShadersDemoStateHolder(
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )

    ShowcaseEntry.ISOMETRIC_GRAPHICS -> createIsometricGraphicsDemoStateHolder(
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )

    ShowcaseEntry.PARTICLES -> createParticlesDemoStateHolder(
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )

    ShowcaseEntry.PERFORMANCE -> createPerformanceDemoStateHolder(
        isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
        sceneEditorConnection = sceneEditorConnection.takeIf { isSceneEditorWindowAvailable },
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )

    ShowcaseEntry.PHYSICS -> createPhysicsDemoStateHolder(
        isSceneEditorEnabled = BuildConfig.IS_SCENE_EDITOR_ENABLED,
        sceneEditorConnection = sceneEditorConnection.takeIf { isSceneEditorWindowAvailable },
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )

    ShowcaseEntry.SHADER_ANIMATIONS -> createShaderAnimationsDemoStateHolder(
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )

    ShowcaseEntry.AUDIO -> createAudioTestStateHolder(
        webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )

    ShowcaseEntry.COLLISION -> createCollisionTestStateHolder(
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )

    ShowcaseEntry.INPUT -> createInputTestStateHolder(
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )

    ShowcaseEntry.LICENSES -> createLicensesScreenStateHolder()

    ShowcaseEntry.ABOUT -> createAboutScreenStateHolder()
}

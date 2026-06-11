/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.logic.manager.ControlManager
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.logic.manager.LogicManager
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.gameplay.resources.TextureResolver
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.manager.VolumetricRenderManager
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.ui.ControlOverlayManager
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageBitmap
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kubriko.examples.demo_isometric_graphics.generated.resources.Res
import kubriko.examples.demo_isometric_graphics.generated.resources.description
import kubriko.examples.demo_isometric_graphics.generated.resources.map_01
import kubriko.examples.demo_isometric_graphics.generated.resources.texture_01

sealed interface IsometricGraphicsDemoStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areImageResourcesLoaded() && areStringResourcesLoaded()

        @Composable
        private fun areImageResourcesLoaded() = preloadedImageBitmap(Res.drawable.texture_01).value != null
                && preloadedImageBitmap(Res.drawable.map_01).value != null

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.description).value.isNotBlank()
    }
}

/**
 * The game runs two independent [Kubriko] instances:
 *  - [logicKubriko] drives the game logic (the main character, the wandering characters, the trees)
 *    in plain Cartesian space.
 *  - [isometricKubriko] only renders: it projects every actor that enters [logicKubriko]'s viewport
 *    into the isometric view via [VolumetricRenderManager].
 * Everything that used to live as module-level singletons in the standalone game is owned here so
 * the demo can be created and disposed like every other Showcase example.
 */
internal class IsometricGraphicsDemoStateHolderImpl(
    isLoggingEnabled: Boolean,
) : IsometricGraphicsDemoStateHolder {

    // region logic instance
    val logicViewportManager = ViewportManager.newInstance(
        initialScaleFactor = 0.04f,
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_LOGIC,
    )
    val controlManager = ControlManager()
    val textureManager = TextureResolver()
    private val logicManager = LogicManager()
    val shouldShowLoadingIndicator = logicManager.shouldShowLoadingIndicator
    private val logicActorManager = ActorManager.newInstance(
        invisibleActorMinimumRefreshTimeInMillis = 500,
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_LOGIC,
    )
    val logicKubriko = Kubriko.newInstance(
        logicActorManager,
        logicViewportManager,
        controlManager,
        logicManager,
        textureManager,
        SpriteManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG_LOGIC,
        ),
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_LOGIC,
    )
    // endregion

    // region isometric (render) instance
    val volumetricViewportManager = ViewportManager.newInstance(
        viewportEdgeBuffer = 100.sceneUnit,
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    val volumetricRenderManager = VolumetricRenderManager(
        allActors = logicActorManager.visibleActorsWithinViewport,
        cameraOffset = controlManager.cameraOffset,
    )
    val controlOverlayManager = ControlOverlayManager(
        controlManager = controlManager,
        logicViewportManager = logicViewportManager,
    )
    val isometricKubriko = Kubriko.newInstance(
        volumetricViewportManager,
        volumetricRenderManager,
        controlOverlayManager,
        KeyboardInputManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        ),
        PointerInputManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        ),
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    // endregion

    private val _kubriko = MutableStateFlow<Kubriko?>(isometricKubriko)
    override val kubriko = _kubriko.asStateFlow()

    override fun dispose() {
        isometricKubriko.dispose()
        logicKubriko.dispose()
    }
}

private const val LOG_TAG = "IsometricGraphics"
private const val LOG_TAG_LOGIC = "IsometricGraphicsLogic"

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

/**
 * What `GeneratedScenes` ([ScenesCodeGenerator]) keeps for the camera Blocks ([CameraBlockCode]): the Actor the camera
 * follows, the shake, and the zoom from before the first Zoom the camera Block, which a new Scene gets back. A new Scene
 * also stops the following and the shaking, so its own settings and Actors decide where the camera is.
 *
 * Blocks run on the game's thread, and in an Actor's added Event on Kubriko's own, and a Scene is loaded on a third, so
 * every field is volatile. The camera moves only while it follows or shakes, and costs nothing per frame otherwise.
 */
internal object CameraCode {
    const val FOLLOW_FUNCTION = "followWithCamera"
    const val MOVE_FUNCTION = "moveCamera"
    const val SHAKE_FUNCTION = "shakeCamera"
    const val ZOOM_FUNCTION = "zoomCamera"
    const val RESET_FUNCTION = "resetCamera"

    fun members() = """
            |
            |    @Volatile
            |    private var cameraTarget: com.pandulapeter.kubriko.actor.traits.Positionable? = null
            |
            |    @Volatile
            |    private var shakeStrength = 0f
            |
            |    @Volatile
            |    private var shakeDurationInMilliseconds = 0f
            |
            |    @Volatile
            |    private var shakeRemainingInMilliseconds = 0f
            |
            |    /**
            |     * How far the camera is from where it would be without the shake.
            |     */
            |    @Volatile
            |    private var shakeOffsetX = 0f
            |
            |    @Volatile
            |    private var shakeOffsetY = 0f
            |
            |    /**
            |     * The zoom the game had before the first Block that zoomed the camera, or NaN while none did.
            |     */
            |    @Volatile
            |    private var scaleFactorBeforeZoom = Float.NaN
            |
            |    /**
            |     * Makes the camera follow [actor] until it moves to a position, follows another Actor, or a new Scene starts.
            |     */
            |    fun $FOLLOW_FUNCTION(actor: com.pandulapeter.kubriko.actor.traits.Positionable) {
            |        if (!isInSceneEditor) cameraTarget = actor
            |    }
            |
            |    /**
            |     * Moves the camera to [x], [y] at once, and stops it following an Actor.
            |     */
            |    fun $MOVE_FUNCTION(x: Float, y: Float) {
            |        if (isInSceneEditor) return
            |        cameraTarget = null
            |        viewportManager.setCameraPosition(SceneOffset((x + shakeOffsetX).sceneUnit, (y + shakeOffsetY).sceneUnit))
            |    }
            |
            |    /**
            |     * Shakes the camera at most [strength] far, less and less for [durationInMilliseconds].
            |     */
            |    fun $SHAKE_FUNCTION(strength: Float, durationInMilliseconds: Float) {
            |        if (isInSceneEditor) return
            |        shakeStrength = strength
            |        shakeDurationInMilliseconds = durationInMilliseconds
            |        shakeRemainingInMilliseconds = durationInMilliseconds
            |    }
            |
            |    /**
            |     * Zooms the camera to [scaleFactor] (1 for the Scene's size), keeping the zoom the game had before for the next Scene.
            |     */
            |    fun $ZOOM_FUNCTION(scaleFactor: Float) {
            |        if (isInSceneEditor || scaleFactor.isNaN() || scaleFactor <= 0f) return
            |        if (scaleFactorBeforeZoom.isNaN()) scaleFactorBeforeZoom = viewportManager.scaleFactor.value.horizontal
            |        viewportManager.setScaleFactor(scaleFactor)
            |    }
            |
            |    private fun $RESET_FUNCTION() {
            |        cameraTarget = null
            |        shakeRemainingInMilliseconds = 0f
            |        // The new Scene's settings place the camera, without the old Scene's shake.
            |        shakeOffsetX = 0f
            |        shakeOffsetY = 0f
            |        val scaleFactor = scaleFactorBeforeZoom
            |        if (!scaleFactor.isNaN()) {
            |            scaleFactorBeforeZoom = Float.NaN
            |            viewportManager.setScaleFactor(scaleFactor)
            |        }
            |    }
            |
            |    override fun onUpdate(deltaTimeInMilliseconds: Int) {
            |        val target = cameraTarget
            |        val isShaking = shakeRemainingInMilliseconds > 0f
            |        if (target == null && !isShaking && shakeOffsetX == 0f && shakeOffsetY == 0f) return
            |        var x: Float
            |        var y: Float
            |        if (target == null) {
            |            val position = viewportManager.cameraPosition.value
            |            x = position.x.raw - shakeOffsetX
            |            y = position.y.raw - shakeOffsetY
            |        } else {
            |            x = target.body.position.x.raw
            |            y = target.body.position.y.raw
            |        }
            |        if (isShaking) {
            |            shakeRemainingInMilliseconds -= deltaTimeInMilliseconds
            |            val reach = shakeStrength * (shakeRemainingInMilliseconds / shakeDurationInMilliseconds).coerceIn(0f, 1f)
            |            shakeOffsetX = (KubrikoWiring.random.nextFloat() * 2f - 1f) * reach
            |            shakeOffsetY = (KubrikoWiring.random.nextFloat() * 2f - 1f) * reach
            |        } else {
            |            shakeOffsetX = 0f
            |            shakeOffsetY = 0f
            |        }
            |        viewportManager.setCameraPosition(SceneOffset((x + shakeOffsetX).sceneUnit, (y + shakeOffsetY).sceneUnit))
            |    }
    """.trimMargin()
}

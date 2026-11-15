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
 * follows and the shake. The zoom is the viewport's own, which a Zoom the camera Block sets until another one does.
 *
 * Blocks run on the game's thread and, in an Actor's added Event, on Kubriko's, and a removed Actor stops the camera
 * following it on Kubriko's too, so every change of the camera's state happens under one short lock. A following Actor
 * passes the Scene it was added in, so one that is still updating after a new Scene was asked for can't take the camera
 * back. Something else moving the camera (a Scene's settings, a Kotlin Block) moves where a shake shakes around. The
 * camera stays still while the game is paused, and costs nothing per frame while it neither follows nor shakes.
 */
internal object CameraCode {
    const val FOLLOW_FUNCTION = "followWithCamera"
    const val UNFOLLOW_FUNCTION = "unfollowWithCamera"
    const val MOVE_FUNCTION = "moveCamera"
    const val SHAKE_FUNCTION = "shakeCamera"
    const val ZOOM_FUNCTION = "zoomCamera"

    fun members() = """
            |
            |    private val cameraLock = ${AtomicsCode.PACKAGE}.AtomicInt(0)
            |    private var cameraTarget: com.pandulapeter.kubriko.actor.traits.Positionable? = null
            |    private var shakeStrength = 0f
            |    private var shakeDurationInMilliseconds = 0f
            |    private var shakeRemainingInMilliseconds = 0f
            |
            |    /**
            |     * Where the camera would be without the shake, and where the shake last put it, which tells whether something
            |     * else moved it since.
            |     */
            |    private var cameraBaseX = 0f
            |    private var cameraBaseY = 0f
            |    private var shakenCameraX = Float.NaN
            |    private var shakenCameraY = Float.NaN
            |
            |    private inline fun <T> withCameraLock(action: () -> T): T {
            |        while (!cameraLock.compareAndSet(0, 1)) Unit
            |        try {
            |            return action()
            |        } finally {
            |            cameraLock.store(0)
            |        }
            |    }
            |
            |    /**
            |     * Makes the camera follow [actor] if it belongs to the Scene shown ([sceneChangesWhenAdded] is [sceneChanges]),
            |     * until it moves to a position, follows another Actor, or [actor] is removed.
            |     */
            |    fun $FOLLOW_FUNCTION(actor: com.pandulapeter.kubriko.actor.traits.Positionable, sceneChangesWhenAdded: Int) {
            |        if (isInSceneEditor) return
            |        withCameraLock { if (sceneChangesWhenAdded == sceneChanges) cameraTarget = actor }
            |    }
            |
            |    /**
            |     * Stops the camera following [actor], which was removed, if it follows it.
            |     */
            |    fun $UNFOLLOW_FUNCTION(actor: com.pandulapeter.kubriko.actor.traits.Positionable) = withCameraLock {
            |        if (cameraTarget === actor) cameraTarget = null
            |    }
            |
            |    /**
            |     * Moves the camera to [x], [y] at once, and stops it following an Actor.
            |     */
            |    fun $MOVE_FUNCTION(x: Float, y: Float) {
            |        if (isInSceneEditor) return
            |        withCameraLock {
            |            cameraTarget = null
            |            if (shakenCameraX.isNaN()) {
            |                viewportManager.setCameraPosition(SceneOffset(x.sceneUnit, y.sceneUnit))
            |            } else {
            |                placeShakenCamera(shakenCameraX - cameraBaseX + x, shakenCameraY - cameraBaseY + y)
            |            }
            |            cameraBaseX = x
            |            cameraBaseY = y
            |        }
            |    }
            |
            |    /**
            |     * Shakes the camera at most [strength] far, less and less for [durationInMilliseconds], starting over if it shakes.
            |     */
            |    fun $SHAKE_FUNCTION(strength: Float, durationInMilliseconds: Float) {
            |        if (isInSceneEditor || !(strength > 0f && durationInMilliseconds > 0f)) return
            |        withCameraLock {
            |            if (cameraTarget == null && shakenCameraX.isNaN()) {
            |                val position = viewportManager.cameraPosition.value
            |                cameraBaseX = position.x.raw
            |                cameraBaseY = position.y.raw
            |            }
            |            shakeStrength = strength
            |            shakeDurationInMilliseconds = durationInMilliseconds
            |            shakeRemainingInMilliseconds = durationInMilliseconds
            |        }
            |    }
            |
            |    /**
            |     * Zooms the camera to [scaleFactor] (1 for the Scene's size) until another zoom.
            |     */
            |    fun $ZOOM_FUNCTION(scaleFactor: Float) {
            |        if (!isInSceneEditor && scaleFactor > 0f) viewportManager.setScaleFactor(scaleFactor)
            |    }
            |
            |    override fun onUpdate(deltaTimeInMilliseconds: Int) {
            |        if (isInSceneEditor || !stateManager.isRunning.value) return
            |        withCameraLock {
            |            val target = cameraTarget
            |            if (target == null && shakenCameraX.isNaN() && shakeRemainingInMilliseconds <= 0f) return
            |            if (target != null) {
            |                cameraBaseX = target.body.position.x.raw
            |                cameraBaseY = target.body.position.y.raw
            |            } else if (!shakenCameraX.isNaN()) {
            |                val position = viewportManager.cameraPosition.value
            |                if (position.x.raw != shakenCameraX || position.y.raw != shakenCameraY) {
            |                    cameraBaseX = position.x.raw
            |                    cameraBaseY = position.y.raw
            |                }
            |            }
            |            if (shakeRemainingInMilliseconds > 0f) {
            |                shakeRemainingInMilliseconds -= deltaTimeInMilliseconds
            |                val reach = shakeStrength * (shakeRemainingInMilliseconds / shakeDurationInMilliseconds).coerceIn(0f, 1f)
            |                placeShakenCamera(
            |                    cameraBaseX + (KubrikoWiring.random.nextFloat() * 2f - 1f) * reach,
            |                    cameraBaseY + (KubrikoWiring.random.nextFloat() * 2f - 1f) * reach,
            |                )
            |            } else {
            |                shakenCameraX = Float.NaN
            |                shakenCameraY = Float.NaN
            |                viewportManager.setCameraPosition(SceneOffset(cameraBaseX.sceneUnit, cameraBaseY.sceneUnit))
            |            }
            |        }
            |    }
            |
            |    private fun placeShakenCamera(x: Float, y: Float) {
            |        shakenCameraX = x
            |        shakenCameraY = y
            |        viewportManager.setCameraPosition(SceneOffset(x.sceneUnit, y.sceneUnit))
            |    }
    """.trimMargin()
}

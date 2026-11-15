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
 * back. Something else moving the camera (a Scene's settings, a Kotlin Block) moves where a shake shakes around. A shake
 * holds still while the game is paused, following goes on, and neither costs anything per frame while it doesn't happen.
 */
internal object CameraCode {
    const val FOLLOW_FUNCTION = "followWithCamera"
    const val UNFOLLOW_FUNCTION = "unfollowWithCamera"
    const val MOVE_FUNCTION = "moveCamera"
    const val SHAKE_FUNCTION = "shakeCamera"
    const val ZOOM_FUNCTION = "zoomCamera"
    const val START_SCENE_FUNCTION = "startScene"
    const val FOLLOWER_INTERFACE = "CameraFollower"

    /**
     * What an Actor's [ScenesCodeGenerator.SCENE_CHANGES_WHEN_ADDED_FIELD] becomes once it is removed.
     */
    const val NOT_IN_SCENE = "NOT_IN_SCENE"
    private const val SCENE_CHANGES_FIELD = ScenesCodeGenerator.SCENE_CHANGES_FIELD

    fun members() = """
            |
            |    /**
            |     * An Actor that uses the camera Blocks: [sceneChangesWhenAdded] is what [sceneChanges] was when it was added, or
            |     * [$NOT_IN_SCENE] once it is removed, so that only the Actors of the Scene shown move the camera.
            |     */
            |    interface $FOLLOWER_INTERFACE : com.pandulapeter.kubriko.actor.traits.Positionable {
            |        val sceneChangesWhenAdded: Int
            |    }
            |
            |    private val cameraLock = ${AtomicsCode.PACKAGE}.AtomicInt(0)
            |
            |    @Volatile
            |    private var cameraTarget: $FOLLOWER_INTERFACE? = null
            |    private var shakeStrength = 0f
            |    private var shakeDurationInMilliseconds = 0f
            |    private var shakeRemainingInMilliseconds = 0f
            |    private var shakeOffsetX = 0f
            |    private var shakeOffsetY = 0f
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
            |    /**
            |     * Held only for a few field writes and a camera update, none of which waits, so spinning costs less than a lock.
            |     */
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
            |     * Whether a camera Block of [caller] (null for a Manager's) may move the camera: [caller] belongs to the Scene shown.
            |     */
            |    private fun isOfSceneShown(caller: $FOLLOWER_INTERFACE?) = caller == null || caller.sceneChangesWhenAdded == $SCENE_CHANGES_FIELD
            |
            |    /**
            |     * Counts a new Scene, which stops the camera following the last one's Actors and shaking, so that the new Scene's
            |     * settings place it.
            |     */
            |    private fun $START_SCENE_FUNCTION() = withCameraLock {
            |        $SCENE_CHANGES_FIELD++
            |        cameraTarget = null
            |        shakeRemainingInMilliseconds = 0f
            |        shakenCameraX = Float.NaN
            |        shakenCameraY = Float.NaN
            |    }
            |
            |    /**
            |     * Makes the camera follow [actor] until it moves to a position, follows another Actor, or [actor] is removed.
            |     */
            |    fun $FOLLOW_FUNCTION(actor: $FOLLOWER_INTERFACE) {
            |        if (isInSceneEditor || cameraTarget === actor) return
            |        withCameraLock { if (isOfSceneShown(actor)) cameraTarget = actor }
            |    }
            |
            |    /**
            |     * Stops the camera following [actor], which was removed, if it follows it.
            |     */
            |    fun $UNFOLLOW_FUNCTION(actor: $FOLLOWER_INTERFACE) = withCameraLock {
            |        if (cameraTarget === actor) cameraTarget = null
            |    }
            |
            |    /**
            |     * Moves the camera to [x], [y] at once, and stops it following an Actor; a position that isn't a number is left out.
            |     */
            |    fun $MOVE_FUNCTION(x: Float, y: Float, caller: $FOLLOWER_INTERFACE? = null) {
            |        if (isInSceneEditor || !x.isFinite() || !y.isFinite()) return
            |        withCameraLock {
            |            if (!isOfSceneShown(caller)) return@withCameraLock
            |            cameraTarget = null
            |            if (shakenCameraX.isNaN()) {
            |                viewportManager.setCameraPosition(SceneOffset(x.sceneUnit, y.sceneUnit))
            |            } else {
            |                placeShakenCamera(x + shakeOffsetX, y + shakeOffsetY)
            |            }
            |            cameraBaseX = x
            |            cameraBaseY = y
            |        }
            |    }
            |
            |    /**
            |     * Shakes the camera at most [strength] far, less and less for [durationInMilliseconds], starting over if it shakes;
            |     * no strength or no time stops the shake.
            |     */
            |    fun $SHAKE_FUNCTION(strength: Float, durationInMilliseconds: Float, caller: $FOLLOWER_INTERFACE? = null) {
            |        if (isInSceneEditor || strength.isNaN() || durationInMilliseconds.isNaN()) return
            |        withCameraLock {
            |            if (!isOfSceneShown(caller)) return@withCameraLock
            |            if (strength <= 0f || durationInMilliseconds <= 0f) {
            |                shakeRemainingInMilliseconds = 0f
            |                return@withCameraLock
            |            }
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
            |    fun $ZOOM_FUNCTION(scaleFactor: Float, caller: $FOLLOWER_INTERFACE? = null) {
            |        if (isInSceneEditor || !(scaleFactor > 0f)) return
            |        withCameraLock { if (isOfSceneShown(caller)) viewportManager.setScaleFactor(scaleFactor) }
            |    }
            |
            |    override fun onUpdate(deltaTimeInMilliseconds: Int) {
            |        if (isInSceneEditor) return
            |        // Kubriko keeps updating Actors while the game isn't running if it is told to, so the camera keeps following them.
            |        val isRunning = stateManager.isRunning.value
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
            |                if (isRunning) {
            |                    shakeRemainingInMilliseconds -= deltaTimeInMilliseconds
            |                    val reach = shakeStrength * (shakeRemainingInMilliseconds / shakeDurationInMilliseconds).coerceIn(0f, 1f)
            |                    shakeOffsetX = (KubrikoWiring.random.nextFloat() * 2f - 1f) * reach
            |                    shakeOffsetY = (KubrikoWiring.random.nextFloat() * 2f - 1f) * reach
            |                }
            |                placeShakenCamera(cameraBaseX + shakeOffsetX, cameraBaseY + shakeOffsetY)
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

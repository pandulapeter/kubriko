/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameWallbreaker.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerId
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.ScoreManager
import com.pandulapeter.kubriko.helpers.extensions.constrainedWithin
import com.pandulapeter.kubriko.helpers.extensions.distanceTo
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.min
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.reflect.KClass

internal class Ball(
    private val paddle: Paddle,
    initialPosition: SceneOffset = SceneOffset(
        x = paddle.body.position.x,
        y = paddle.body.position.y - paddle.body.pivot.y - Radius
    ),
) : Visible, Dynamic, CollisionDetector, PointerInputAware, KeyboardInputAware, Unique {

    override val collidableTypes = listOf<KClass<out Collidable>>(Brick::class, Paddle::class)
    override val body = BoxBody(
        initialPosition = initialPosition,
        initialSize = SceneSize(Radius * 2, Radius * 2),
    )
    override val collisionMask = CircleCollisionMask(
        initialRadius = Radius,
        initialPosition = body.position,
    )
    private var previousPosition = body.position
    private var baseSpeedX = 1
    private var baseSpeedY = -1
    private lateinit var actorManager: ActorManager
    private lateinit var audioManager: AudioManager
    private lateinit var gameManager: GameplayManager
    private lateinit var scoreManager: ScoreManager
    private lateinit var stateManager: StateManager
    private lateinit var viewportManager: ViewportManager
    private var state = State.UNINITIALIZED
    val isLaunched get() = state == State.LAUNCHED
    private var trackingPointerId: PointerId? = null
    private var isCollidingWithPaddle = false

    private enum class State {
        UNINITIALIZED,
        POSITIONING,
        LAUNCHED,
        GAME_OVER,
    }

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        audioManager = kubriko.get()
        gameManager = kubriko.get()
        scoreManager = kubriko.get()
        stateManager = kubriko.get()
        viewportManager = kubriko.get()
        paddle.attachBall(this)
    }

    override fun onRemoved() {
        paddle.detachBall(this)
    }

    /**
     * Keeps a ball that is waiting for launch on the paddle's x. The paddle calls this at the end of its own update (and the
     * ball from its update), so within a frame the ball always reflects the paddle's final position, whatever the actor
     * update order.
     */
    fun snapToPaddle() {
        if (state == State.UNINITIALIZED || state == State.POSITIONING) {
            body.position = SceneOffset(
                x = paddle.body.position.x,
                y = body.position.y,
            ).constrainedWithin(viewportManager.topLeft.value, viewportManager.bottomRight.value)
            collisionMask.position = body.position
        }
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        val viewportTopLeft = viewportManager.topLeft.value
        val viewportBottomRight = viewportManager.bottomRight.value
        when (state) {
            State.UNINITIALIZED, State.POSITIONING -> {
                snapToPaddle()
            }

            State.LAUNCHED -> {
                body.position = body.position.constrainedWithin(viewportTopLeft, viewportBottomRight)
                val speed = min(InitialSpeed + SpeedIncrement * scoreManager.score.value, MaximumSpeed)
                val movementDeltaInMilliseconds = deltaTimeInMilliseconds.coerceAtMost(MAXIMUM_MOVEMENT_DELTA_IN_MILLISECONDS)
                val nextPosition = body.position + SceneOffset(speed * baseSpeedX, speed * baseSpeedY) * movementDeltaInMilliseconds
                var shouldPlayEdgeBounceSoundEffect = false
                if (nextPosition.x < viewportTopLeft.x || nextPosition.x > viewportBottomRight.x) {
                    baseSpeedX *= -1
                    shouldPlayEdgeBounceSoundEffect = true
                }
                if (nextPosition.y < viewportTopLeft.y) {
                    baseSpeedY *= -1
                    shouldPlayEdgeBounceSoundEffect = true
                }
                if (nextPosition.y > viewportBottomRight.y) {
                    state = State.GAME_OVER
                    gameManager.onGameOver()
                    audioManager.playGameOverSoundEffect()
                }
                previousPosition = body.position
                body.position = nextPosition.constrainedWithin(viewportTopLeft, viewportBottomRight)
                if (shouldPlayEdgeBounceSoundEffect) {
                    audioManager.playEdgeBounceSoundEffect()
                }
            }

            State.GAME_OVER -> Unit
        }
        collisionMask.position = body.position
    }

    override fun onPointerPressed(pointerId: PointerId, screenOffset: Offset) {
        if (stateManager.isRunning.value) {
            trackingPointerId = pointerId
            if (state == State.UNINITIALIZED) {
                state = State.POSITIONING
            }
        }
    }

    override fun onPointerReleased(pointerId: PointerId, screenOffset: Offset) {
        if (stateManager.isRunning.value && state == State.POSITIONING && trackingPointerId == pointerId) {
            state = State.LAUNCHED
            audioManager.playPaddleHitSoundEffect()
        }
    }

    override fun onKeyPressed(key: Key) {
        if (stateManager.isRunning.value && state != State.GAME_OVER && key == Key.Spacebar && state != State.LAUNCHED) {
            state = State.LAUNCHED
            audioManager.playPaddleHitSoundEffect()
        }
    }

    override fun onCollisionDetected(collidables: List<Collidable>) {
        var shouldPlayBrickPopSoundEffect = false
        var shouldPlayPaddleHitSoundEffect = false
        if (state == State.LAUNCHED) {
            val collidable = collidables.filterIsInstance<Paddle>().firstOrNull()
                ?: collidables.filterIsInstance<Brick>().filterNot { it.isDestroyed }.minByOrNull { it.body.position.distanceTo(body.position) }
                ?: return
            if (collidable is Brick) {
                body.position = previousPosition
                isCollidingWithPaddle = false
                shouldPlayBrickPopSoundEffect = true
                collidable.isDestroyed = true
                actorManager.remove(collidable)
                actorManager.add(
                    BrickPopEffect(
                        position = collidable.body.position,
                        hue = collidable.hue,
                    )
                )
                scoreManager.incrementScore()
                gameManager.onBrickDestroyed()
            }
            val boundingBox = collidable.body.axisAlignedBoundingBox
            bounceRegion(
                ballX = body.position.x,
                ballY = body.position.y,
                left = boundingBox.left,
                top = boundingBox.top,
                right = boundingBox.right,
                bottom = boundingBox.bottom,
            )?.let { region ->
                baseSpeedX = region.bouncedSpeedX(baseSpeedX)
                baseSpeedY = region.bouncedSpeedY(baseSpeedY)
            }
            if (collidable is Paddle) {
                if (!isCollidingWithPaddle) {
                    isCollidingWithPaddle = true
                    shouldPlayPaddleHitSoundEffect = true
                }
                if (body.position.y < collidable.body.axisAlignedBoundingBox.top) {
                    body.position = SceneOffset(
                        x = body.position.x,
                        y = paddle.body.position.y - collidable.body.pivot.y - Radius,
                    )
                } else {
                    if (body.position.x <= collidable.body.axisAlignedBoundingBox.left) {
                        body.position = SceneOffset(
                            x = collidable.body.axisAlignedBoundingBox.left - Radius,
                            y = body.position.y,
                        )
                    } else if (body.position.x >= collidable.body.axisAlignedBoundingBox.right) {
                        body.position = SceneOffset(
                            x = collidable.body.axisAlignedBoundingBox.right + Radius,
                            y = body.position.y,
                        )
                    }
                }
            }
            if (shouldPlayBrickPopSoundEffect) {
                audioManager.playBrickPopSoundEffect()
            }
            if (shouldPlayPaddleHitSoundEffect) {
                audioManager.playPaddleHitSoundEffect()
            }
        }
    }

    override fun DrawScope.draw() {
        if (state != State.GAME_OVER) {
            drawCircle(
                color = Color.White,
                radius = Radius.raw,
                center = body.pivot.raw,
            )
            drawCircle(
                color = Color.Black,
                radius = Radius.raw,
                center = body.pivot.raw,
                style = Stroke(width = 3f),
            )
        }
    }

    companion object {
        private val InitialSpeed = 0.6f.sceneUnit
        private val SpeedIncrement = 0.005f.sceneUnit
        internal val MaximumSpeed = 1.8f.sceneUnit
        internal val Radius = 20f.sceneUnit

        /**
         * Caps the time a single tick moves the ball by, so a long frame cannot carry it past the paddle or a brick row.
         */
        const val MAXIMUM_MOVEMENT_DELTA_IN_MILLISECONDS = 33
    }
}

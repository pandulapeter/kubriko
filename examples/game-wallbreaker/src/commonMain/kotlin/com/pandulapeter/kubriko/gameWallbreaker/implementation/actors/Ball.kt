package com.pandulapeter.kubriko.gameWallbreaker.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.gameWallbreaker.implementation.WallbreakerGameManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerAudioManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerScoreManager
import com.pandulapeter.kubriko.implementation.extensions.constrainedWithin
import com.pandulapeter.kubriko.implementation.extensions.min
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.reflect.KClass

internal class Ball(
    private val paddle: Visible,
    initialPosition: SceneOffset = SceneOffset(
        x = paddle.body.position.x,
        y = paddle.body.position.y - paddle.body.pivot.y - Radius
    ),
) : Visible, Dynamic, CollisionDetector, PointerInputAware {

    private var isGameOver = false
    private var isMoving = false
    override val collidableTypes = listOf<KClass<out Collidable>>(Brick::class, Paddle::class)
    override val body = CircleBody(
        initialPosition = initialPosition,
        initialRadius = Radius,
    )
    private var previousPosition = body.position
    private var baseSpeedX = 1
    private var baseSpeedY = 1
    private lateinit var actorManager: ActorManager
    private lateinit var audioManager: WallbreakerAudioManager
    private lateinit var gameManager: WallbreakerGameManager
    private lateinit var scoreManager: WallbreakerScoreManager
    private lateinit var viewportManager: ViewportManager
    private var isCollidingWithPaddle = false

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.require()
        audioManager = kubriko.require()
        gameManager = kubriko.require()
        scoreManager = kubriko.require()
        viewportManager = kubriko.require()
    }

    override fun update(deltaTimeInMillis: Float) {
        val viewportTopLeft = viewportManager.topLeft.value
        val viewportBottomRight = viewportManager.bottomRight.value
        if (!isMoving) {
            body.position = SceneOffset(
                x = paddle.body.position.x,
                y = body.position.y,
            ).constrainedWithin(viewportTopLeft, viewportBottomRight)
        } else {
            if (!isGameOver) {
                body.position = body.position.constrainedWithin(viewportTopLeft, viewportBottomRight)
                val speed = min(InitialSpeed + SpeedIncrement * scoreManager.score.value, MaximumSpeed)
                val nextPosition = body.position + SceneOffset(speed * baseSpeedX, speed * baseSpeedY) * deltaTimeInMillis
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
                    isGameOver = true
                    gameManager.onGameOver()
                    audioManager.playGameOverSoundEffect()
                }
                previousPosition = body.position
                body.position = nextPosition.constrainedWithin(viewportTopLeft, viewportBottomRight)
                if (shouldPlayEdgeBounceSoundEffect) {
                    audioManager.playEdgeBounceSoundEffect()
                }
            }
        }
    }

    override fun onPointerPress(screenOffset: Offset) {
        isMoving = true
    }

    override fun onCollisionDetected(collidables: List<Collidable>) {
        var shouldPlayBrickPopSoundEffect = false
        var shouldPlayPaddleHitSoundEffect = false
        if (isMoving && !isGameOver) {
            body.position = previousPosition
            collidables.forEach { collidable ->
                when (collidable) {
                    is Brick -> {
                        shouldPlayBrickPopSoundEffect = true
                        actorManager.remove(collidable)
                        actorManager.add(
                            BrickPopEffect(
                                position = collidable.body.position,
                                hue = collidable.hue,
                            )
                        )
                        scoreManager.incrementScore()
                        isCollidingWithPaddle = false
                    }

                    is Paddle -> {
                        shouldPlayPaddleHitSoundEffect = true
                        if (isCollidingWithPaddle) {
                            baseSpeedY = -1
                            body.position = SceneOffset(
                                x = body.position.x,
                                y = paddle.body.position.y - paddle.body.pivot.y - Radius
                            )
                            return@forEach
                        } else {
                            isCollidingWithPaddle = true
                        }
                    }
                }
                when {
                    body.position.y.raw in collidable.body.axisAlignedBoundingBox.min.y..collidable.body.axisAlignedBoundingBox.max.y -> baseSpeedX *= -1
                    body.position.x.raw in collidable.body.axisAlignedBoundingBox.min.x..collidable.body.axisAlignedBoundingBox.max.x -> baseSpeedY *= -1
                    else -> {
                        baseSpeedX *= -1
                        baseSpeedY *= -1
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
        if (!isGameOver) {
            drawCircle(
                color = Color.LightGray,
                radius = Radius.raw,
                center = body.pivot.raw,
            )
            drawCircle(
                color = Color.Black,
                radius = Radius.raw,
                center = body.pivot.raw,
                style = Stroke(),
            )
        }
    }

    companion object {
        private val InitialSpeed = 0.6f.sceneUnit
        private val SpeedIncrement = 0.005f.sceneUnit
        private val MaximumSpeed = 1.8f.sceneUnit
        private val Radius = 20f.sceneUnit
    }
}

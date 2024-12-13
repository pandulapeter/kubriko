package com.pandulapeter.kubriko.gameWallbreaker.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerAudioManager
import com.pandulapeter.kubriko.implementation.extensions.constrainedWithin
import com.pandulapeter.kubriko.implementation.extensions.distanceTo
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
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
    override val body = RectangleBody(
        initialPosition = initialPosition,
        initialSize = SceneSize(Radius * 2, Radius * 2),
    )
    private var previousPosition = body.position
    private var speedX = Speed
    private var speedY = Speed
    private lateinit var actorManager: ActorManager
    private lateinit var wallbreakerAudioManager: WallbreakerAudioManager
    private lateinit var viewportManager: ViewportManager
    private var isCollidingWithPaddle = false

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.require()
        wallbreakerAudioManager = kubriko.require()
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
                val nextPosition = body.position + SceneOffset(speedX, speedY) * deltaTimeInMillis
                var shouldPlaySound = false
                if (nextPosition.x < viewportTopLeft.x || nextPosition.x > viewportBottomRight.x) {
                    speedX *= -1
                    shouldPlaySound = true
                }
                if (nextPosition.y < viewportTopLeft.y) {
                    speedY *= -1
                    shouldPlaySound = true
                }
                if (nextPosition.y > viewportBottomRight.y) {
                    isGameOver = true
                    wallbreakerAudioManager.playGameOverSound()
                }
                previousPosition = body.position
                body.position = nextPosition.constrainedWithin(viewportTopLeft, viewportBottomRight)
                if (shouldPlaySound) {
                    wallbreakerAudioManager.playEdgeBounceSound()
                }
            }
        }
    }

    override fun onPointerPress(screenOffset: Offset) {
        isMoving = true
    }

    // TODO: We should predict collisions instead of only treating them afterwards
    override fun onCollisionDetected(collidables: List<Collidable>) {
        if (isMoving && !isGameOver) {
            val collidable = collidables.filterIsInstance<Paddle>().firstOrNull() ?: collidables.filterIsInstance<Brick>().minBy { it.body.position.distanceTo(body.position) }
            if (collidable is Paddle && isCollidingWithPaddle) {
                return
            }
            body.position = previousPosition
            isCollidingWithPaddle = false
            when {
                body.position.y.raw in collidable.body.axisAlignedBoundingBox.min.y..collidable.body.axisAlignedBoundingBox.max.y -> speedX *= -1
                body.position.x.raw in collidable.body.axisAlignedBoundingBox.min.x..collidable.body.axisAlignedBoundingBox.max.x -> speedY *= -1
                else -> {
                    speedX *= -1
                    speedY *= -1
                }
            }
            if (collidable is Brick) {
                actorManager.remove(collidable)
                actorManager.add(
                    BrickPopEffect(
                        position = collidable.body.position,
                        hue = collidable.hue,
                    )
                )
            }
            if (collidable is Paddle) {
                wallbreakerAudioManager.playPaddleHitSound()
                isCollidingWithPaddle = true
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
        private val Speed = 0.8f.sceneUnit
        private val Radius = 20f.sceneUnit
    }
}

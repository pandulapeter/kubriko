package com.pandulapeter.kubrikoWallbreaker.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.constrainedWithin
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

internal class Ball(
    private val radius: ScenePixel = 20f.scenePixel,
    speed: ScenePixel = 0.4f.scenePixel,
    private val bricks: StateFlow<List<Brick>>,
) : Visible, Dynamic {

    override val boundingBox: SceneSize = SceneSize(radius * 2, radius * 2)
    override var position: SceneOffset = SceneOffset(0f.scenePixel, (-400f).scenePixel)
    private var speedX = speed
    private var speedY = speed
    private lateinit var actorManager: ActorManager
    private lateinit var viewportManager: ViewportManager

    override fun onAdd(kubriko: Kubriko) {
        actorManager = kubriko.require()
        viewportManager = kubriko.require()
    }

    override fun update(deltaTimeInMillis: Float) {
        val viewportTopLeft = viewportManager.topLeft.value
        val viewportBottomRight = viewportManager.bottomRight.value
        position = position.constrainedWithin(viewportTopLeft, viewportBottomRight)
        val nextPosition = position + SceneOffset(speedX, speedY) * deltaTimeInMillis
        if (nextPosition.x < viewportTopLeft.x || nextPosition.x > viewportBottomRight.x) {
            speedX *= -1
        }
        if (nextPosition.y < viewportTopLeft.y || nextPosition.y > viewportBottomRight.y) {
            speedY *= -1
        }
        bricks.value.firstOrNull { isOverlappingBrick(position = nextPosition, brick = it) }?.let { brick ->
            handleCollision(
                nextPosition = nextPosition,
                brickPosition = brick.position,
                brickBoundingBox = brick.boundingBox,
            )
            actorManager.remove(brick)
            actorManager.add(
                BrickDestructionEffect(
                    position = brick.position,
                    hue = brick.hue,
                )
            )
        }
        position = nextPosition
    }

    private fun isOverlappingBrick(
        position: SceneOffset,
        brick: Brick,
    ): Boolean {
        val brickTopLeft = brick.position - brick.pivotOffset
        val brickBottomRight = brickTopLeft + SceneOffset(brick.boundingBox.width, brick.boundingBox.height)
        val closestX = maxOf(brickTopLeft.x, minOf(position.x, brickBottomRight.x))
        val closestY = maxOf(brickTopLeft.y, minOf(position.y, brickBottomRight.y))
        val distanceSquared = (position.x - closestX) * (position.x - closestX) + (position.y - closestY) * (position.y - closestY)
        return distanceSquared <= radius * radius
    }

    private fun handleCollision(
        nextPosition: SceneOffset,
        brickPosition: SceneOffset,
        brickBoundingBox: SceneSize,
    ) {
        val overlapX = (brickBoundingBox.width.raw / 2) - abs((nextPosition.x - brickPosition.x).raw)
        val overlapY = (brickBoundingBox.height.raw / 2) - abs((nextPosition.y - brickPosition.y).raw)
        when {
            overlapX < overlapY -> speedX *= -1
            overlapY < overlapX -> speedY *= -1
            else -> {
                speedX *= -1
                speedY *= -1
            }
        }
    }

    override fun DrawScope.draw() {
        drawCircle(
            color = Color.LightGray,
            radius = radius.raw,
            center = pivotOffset.raw,
        )
        drawCircle(
            color = Color.Black,
            radius = radius.raw,
            center = pivotOffset.raw,
            style = Stroke(),
        )
    }
}

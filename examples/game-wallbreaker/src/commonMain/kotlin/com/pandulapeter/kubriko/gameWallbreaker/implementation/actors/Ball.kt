package com.pandulapeter.kubriko.gameWallbreaker.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.implementation.extensions.constrainedWithin
import com.pandulapeter.kubriko.implementation.extensions.distanceTo
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize

internal class Ball(
    private val radius: ScenePixel = 20f.scenePixel,
    speed: ScenePixel = 0.8f.scenePixel,
) : CollisionDetector, Visible, Dynamic {

    override val collidableTypes = listOf(Brick::class)
    override val boundingBox: SceneSize = SceneSize(radius * 2, radius * 2)
    override var position: SceneOffset = SceneOffset(0f.scenePixel, (-400f).scenePixel)
    private var previousPosition = position
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
        previousPosition = position
        position = nextPosition
    }

    // TODO: We could predict collisions instead of only treating them afterwards
    override fun onCollisionDetected(collidables: List<Collidable>) {
        position = previousPosition
        val brick = collidables.filterIsInstance<Brick>().minBy { it.position.distanceTo(position) }
        val brickTopLeft = brick.position - brick.pivotOffset
        val brickBottomRight = brickTopLeft + SceneOffset(brick.boundingBox.width, brick.boundingBox.height)
        when {
            position.y.raw in brickTopLeft.y..brickBottomRight.y -> speedX *= -1
            position.x.raw in brickTopLeft.x..brickBottomRight.x -> speedY *= -1
            else -> {
                speedX *= -1
                speedY *= -1
            }
        }
        actorManager.remove(brick)
        actorManager.add(
            BrickDestructionEffect(
                position = brick.position,
                hue = brick.hue,
            )
        )
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

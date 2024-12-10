package com.pandulapeter.kubriko.gameWallbreaker.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.audioPlayback.AudioPlaybackManager
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.implementation.extensions.constrainedWithin
import com.pandulapeter.kubriko.implementation.extensions.distanceTo
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import com.pandulapeter.kubriko.types.SceneSize
import kubriko.examples.game_wallbreaker.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

internal class Ball(
    private val radius: SceneUnit = 20f.sceneUnit,
    speed: SceneUnit = 0.8f.sceneUnit,
) : Visible, Dynamic, CollisionDetector {

    override val collidableTypes = listOf(Brick::class)
    override val body = RectangleBody(
        initialPosition = SceneOffset(0f.sceneUnit, (-400f).sceneUnit),
        initialSize = SceneSize(radius * 2, radius * 2),
    )
    private var previousPosition = body.position
    private var speedX = speed
    private var speedY = speed
    private lateinit var actorManager: ActorManager
    private lateinit var audioPlaybackManager: AudioPlaybackManager
    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.require()
        audioPlaybackManager = kubriko.require()
        viewportManager = kubriko.require()
    }

    override fun update(deltaTimeInMillis: Float) {
        val viewportTopLeft = viewportManager.topLeft.value
        val viewportBottomRight = viewportManager.bottomRight.value
        body.position = body.position.constrainedWithin(viewportTopLeft, viewportBottomRight)
        val nextPosition = body.position + SceneOffset(speedX, speedY) * deltaTimeInMillis
        var shouldPlaySound = false
        if (nextPosition.x < viewportTopLeft.x || nextPosition.x > viewportBottomRight.x) {
            speedX *= -1
            shouldPlaySound = true
        }
        if (nextPosition.y < viewportTopLeft.y || nextPosition.y > viewportBottomRight.y) {
            speedY *= -1
            shouldPlaySound = true
        }
        previousPosition = body.position
        body.position = nextPosition
        if (shouldPlaySound) {
            @OptIn(ExperimentalResourceApi::class)
            audioPlaybackManager.playSound(Res.getUri("files/sounds/click.wav"))
        }
    }

    // TODO: We could predict collisions instead of only treating them afterwards
    override fun onCollisionDetected(collidables: List<Collidable>) {
        body.position = previousPosition
        val brick = collidables.filterIsInstance<Brick>().minBy { it.body.position.distanceTo(body.position) }
        when {
            body.position.y.raw in brick.body.axisAlignedBoundingBox.min.y..brick.body.axisAlignedBoundingBox.max.y -> speedX *= -1
            body.position.x.raw in brick.body.axisAlignedBoundingBox.min.x..brick.body.axisAlignedBoundingBox.max.x -> speedY *= -1
            else -> {
                speedX *= -1
                speedY *= -1
            }
        }
        actorManager.remove(brick)
        actorManager.add(
            BrickDestructionEffect(
                position = brick.body.position,
                hue = brick.hue,
            )
        )
    }

    override fun DrawScope.draw() {
        drawCircle(
            color = Color.LightGray,
            radius = radius.raw,
            center = body.pivot.raw,
        )
        drawCircle(
            color = Color.Black,
            radius = radius.raw,
            center = body.pivot.raw,
            style = Stroke(),
        )
    }
}

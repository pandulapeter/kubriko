package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.actor.traits.InsetPaddingAware
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.distanceTo
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.toSceneOffset
import com.pandulapeter.kubriko.gameSpaceSquadron.ViewportHeight
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.sprite_ship
import kotlin.math.abs
import kotlin.math.hypot

internal class Ship : Visible, Dynamic, InsetPaddingAware, Group {

    private lateinit var spriteManager: SpriteManager
    private lateinit var viewportManager: ViewportManager
    private val animatedSprite = AnimatedSprite(
        getImageBitmap = { spriteManager.loadSprite(Res.drawable.sprite_ship) },
        frameSize = IntSize(128, 144),
        frameCount = 23,
        framesPerRow = 8,
        framesPerSecond = 60f,
    )
    override val body = RectangleBody(
        initialSize = SceneSize(
            width = 128.sceneUnit,
            height = 144.sceneUnit,
        ),
    )
    private val shipDestination = ShipDestination()
    override val actors = listOf(shipDestination)

    override fun onAdded(kubriko: Kubriko) {
        spriteManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun onInsetPaddingChanged(insetPadding: Rect) {
        // TODO: Limit the play area
        val topLeft = insetPadding.topLeft.toSceneOffset(viewportManager)
        val bottomRight = insetPadding.bottomRight.toSceneOffset(viewportManager)
        val offset = SceneOffset(
            x = SceneUnit.Zero,
            y = body.pivot.y * 4,
        )
        body.position = SceneOffset(
            x = (topLeft.x - bottomRight.x) / 2,
            y = (topLeft.y - bottomRight.y) / 2 + ViewportHeight / 2,
        ) - offset
    }

    private var speed = SceneUnit.Zero

    override fun update(deltaTimeInMilliseconds: Float) {
        val previousX = body.position.x
        speed = shipDestination.body.position.distanceTo(body.position) * 0.05f + 0.5f.sceneUnit
        moveTowards(shipDestination.body.position, speed)
        if (body.position.x < previousX) {
            if (!animatedSprite.isLastFrame) {
                animatedSprite.stepForward(deltaTimeInMilliseconds, shouldLoop = false)
            }
            body.scale = Scale(1f, 1f)
        } else if (body.position.x > previousX) {
            if (!animatedSprite.isLastFrame) {
                animatedSprite.stepForward(deltaTimeInMilliseconds, shouldLoop = false)
            }
            body.scale = Scale(-1f, 1f)
        } else {
            if (!animatedSprite.isFirstFrame) {
                animatedSprite.stepBackwards(deltaTimeInMilliseconds, shouldLoop = false)
            }
        }
    }

    private fun moveTowards(target: SceneOffset, speed: SceneUnit) {
        val deltaX = target.x - body.position.x
        val deltaY = target.y - body.position.y
        val distance = hypot(deltaX.raw, deltaY.raw).sceneUnit
        body.position = if (distance <= speed) {
            target
        } else {
            val absDeltaX = abs(deltaX.raw).sceneUnit
            val absDeltaY = abs(deltaY.raw).sceneUnit
            var x = body.position.x
            var y = body.position.y
            if (absDeltaX > absDeltaY) {
                val ratio = absDeltaY / absDeltaX
                x += if (deltaX > SceneUnit.Zero) speed else -speed
                y += if (deltaY > SceneUnit.Zero) speed * ratio else -speed * ratio
            } else {
                val ratio = absDeltaX / absDeltaY
                y += if (deltaY > SceneUnit.Zero) speed else -speed
                x += if (deltaX > SceneUnit.Zero) speed * ratio else -speed * ratio
            }
            SceneOffset(x, y)
        }
    }

    override fun DrawScope.draw() = animatedSprite.draw(this)

    companion object {
        private val Speed = 16.sceneUnit
    }
}
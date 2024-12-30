package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.InsetPaddingAware
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.toSceneOffset
import com.pandulapeter.kubriko.gameSpaceSquadron.ViewportHeight
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.sprite_ship

internal class Ship : Visible, Dynamic, InsetPaddingAware {

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

    override fun onAdded(kubriko: Kubriko) {
        spriteManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun onInsetPaddingChanged(insetPadding: Rect) {
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

    private var isAnimatingForward = true

    override fun update(deltaTimeInMillis: Float) {
        if (isAnimatingForward) {
            animatedSprite.stepForward(deltaTimeInMillis)
            if (animatedSprite.imageIndex == 22) {
                isAnimatingForward = false
            }
        } else {
            animatedSprite.stepBackwards(deltaTimeInMillis)
            if (animatedSprite.imageIndex == 0) {
                isAnimatingForward = true
                body.scale = Scale(-body.scale.horizontal, body.scale.vertical)
            }
        }
    }

    override fun DrawScope.draw() = animatedSprite.draw(this)
}
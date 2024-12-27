package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.sprite_ship

internal class Ship : Visible, Dynamic, InsetPaddingAware {

    private lateinit var spriteManager: SpriteManager
    private lateinit var viewportManager: ViewportManager
    private val sprite = AnimatedSprite(
        getImageBitmap = { spriteManager.loadSprite(Res.drawable.sprite_ship) },
        frameSize = IntSize(130, 146),
        frameCount = 23,
        framesPerColumn = 4,
        framesPerSecond = 10f,
    )

    override fun onAdded(kubriko: Kubriko) {
        spriteManager = kubriko.get()
        viewportManager = kubriko.get()
        update(0f)
    }

    override val body = RectangleBody(
        initialSize = SceneSize(
            width = 130.sceneUnit,
            height = 146.sceneUnit,
        ),
    )

    override fun update(deltaTimeInMillis: Float) {
        sprite.stepForward(deltaTimeInMillis)
    }

    override fun onInsetPaddingChanged(insetPadding: Rect) {
        val topLeft = insetPadding.topLeft.toSceneOffset(viewportManager)
        val bottomRight = insetPadding.bottomRight.toSceneOffset(viewportManager)
        val offset = Offset(0f, 250f).toSceneOffset(viewportManager)
        body.position = SceneOffset(
            x = topLeft.x - bottomRight.x,
            y = ViewportHeight / 2 + topLeft.y - bottomRight.y,
        ) - offset
    }

    override fun DrawScope.draw() = sprite.draw(this)
}
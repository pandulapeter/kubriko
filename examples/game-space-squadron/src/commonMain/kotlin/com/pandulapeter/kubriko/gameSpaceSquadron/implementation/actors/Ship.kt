package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.InsetPaddingAware
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.gameSpaceSquadron.ViewportHeight
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.sprite_ship
import kotlin.math.roundToInt

internal class Ship : Visible, Dynamic, InsetPaddingAware {

    private lateinit var spriteManager: SpriteManager
    private lateinit var viewportManager: ViewportManager
    private var imageIndex = 0f

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
        imageIndex += 0.01f * deltaTimeInMillis
        if (imageIndex > 22) {
            imageIndex = 0f
        }
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

    override fun DrawScope.draw() {
        // TODO: Create an AnimatedSprite wrapper that takes care of the calculations
        spriteManager.loadSprite(Res.drawable.sprite_ship)?.let {
            val x = imageIndex.roundToInt() / 4
            val y = imageIndex.roundToInt() % 4
            drawImage(
                image = it,
                srcOffset = IntOffset(130 * x, 146 * y),
                srcSize = IntSize(130, 146),
            )
        } ?: drawRect(
            color = Color.White,
            size = body.size.raw,
        )
    }
}
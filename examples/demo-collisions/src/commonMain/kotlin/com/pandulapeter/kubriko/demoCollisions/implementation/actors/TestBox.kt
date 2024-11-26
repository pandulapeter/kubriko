package com.pandulapeter.kubriko.demoCollisions.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.sin

internal class TestBox(
    position: SceneOffset,
    val hue: Float,
) : Visible, Collidable, Dynamic {
    override val body = RectangleBody(
        initialPosition = position,
        initialSize = SceneSize(Width, Height),
    )
    private val color = Color.hsv(hue, 0.2f, 0.9f)

    override fun DrawScope.draw() {
        drawRect(
            color = color,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            size = body.size.raw,
            style = Stroke(),
        )
    }

    private var acc = 0f

    override fun update(deltaTimeInMillis: Float) {
        body.rotation += (deltaTimeInMillis * 0.001f).rad
        acc += deltaTimeInMillis
        body.scale = (1f + 2f * sin(acc * 0.001f)).let {
            Scale(it, it)
        }
    }

    companion object {
        val Width = 100f.scenePixel
        val Height = 40f.scenePixel
    }
}

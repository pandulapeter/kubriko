package com.pandulapeter.kubriko.demoCollisions.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.cos
import kotlin.math.sin

internal class TestBox(
    position: SceneOffset,
    hue: Float,
) : Visible, Collidable, Dynamic {
    override val body = RectangleBody(
        initialPosition = position,
        initialSize = SceneSize(Width, Height),
    )
    private val color = Color.hsv(hue, 0.2f, 0.9f)
    private var acc = 0f

    override fun DrawScope.draw() = drawRect(
        color = color,
        size = body.size.raw,
    )

    override fun update(deltaTimeInMillis: Float) {
        body.rotation += (deltaTimeInMillis * 0.001f).rad
        acc += deltaTimeInMillis
        body.scale = Scale(
            horizontal = 1f + 2f * cos(acc * 0.001f),
            vertical = 1f + 2f * sin(acc * 0.001f)
        )
    }

    companion object {
        val Width = 100f.scenePixel
        val Height = 40f.scenePixel
    }
}

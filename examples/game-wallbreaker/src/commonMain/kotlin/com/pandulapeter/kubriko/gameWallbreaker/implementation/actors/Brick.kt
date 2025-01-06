package com.pandulapeter.kubriko.gameWallbreaker.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class Brick(
    position: SceneOffset,
) : Visible, Collidable {
    override val body = RectangleBody(
        initialPosition = position,
        initialSize = SceneSize(Width, Height),
    )
    var hue = randomHue()
        private set(value) {
            field = value
            color = createColor()
        }
    private var color = createColor()

    fun randomizeHue() {
        hue = randomHue()
    }

    private fun randomHue() = (0..360).random().toFloat()

    private fun createColor() = Color.hsv(hue, 0.3f, 1f)

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

    companion object {
        val Width = 100f.sceneUnit
        val Height = 40f.sceneUnit
    }
}

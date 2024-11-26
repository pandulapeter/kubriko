package com.pandulapeter.kubriko.demoCollisions.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.math.cos
import kotlin.math.sin

internal class TestCircle(
    position: SceneOffset,
    hue: Float,
) : Visible, Collidable, Dynamic {
    override val body = CircleBody(
        initialPosition = position,
        initialRadius = Radius,
    )
    private val color = Color.hsv(hue, 0.2f, 0.9f)

    override fun DrawScope.draw() {
        drawCircle(
            color = color,
            radius = body.radius.raw,
            center = body.pivot.raw,
        )
        drawCircle(
            color = Color.Black,
            radius = body.radius.raw,
            center = body.pivot.raw,
            style = Stroke(),
        )
        drawLine(
            color = Color.Black,
            start = Offset(body.pivot.raw.x - body.radius.raw, body.pivot.raw.y),
            end = Offset(body.pivot.raw.x + body.radius.raw, body.pivot.raw.y),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.Black,
            start = Offset(body.pivot.raw.x, body.pivot.raw.y - body.radius.raw),
            end = Offset(body.pivot.raw.x, body.pivot.raw.y + body.radius.raw),
            strokeWidth = 2f,
        )
    }

    private var acc = 0f

    override fun update(deltaTimeInMillis: Float) {
        body.rotation += (deltaTimeInMillis * 0.001f).rad
        acc += deltaTimeInMillis
        body.scale = Scale(
            horizontal = 1f + 2f * cos(acc * 0.001f),
            vertical = 1f + 2f * sin(acc * 0.001f)
        )
    }

    companion object {
        val Radius = 50f.scenePixel
    }
}

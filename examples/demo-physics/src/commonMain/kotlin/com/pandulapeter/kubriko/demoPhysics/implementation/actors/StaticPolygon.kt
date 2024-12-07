package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.PolygonBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.types.SceneOffset

internal class StaticPolygon(
    initialOffset: SceneOffset,
    shape: Polygon,
    private val isRotating: Boolean,
) : RigidBody, Dynamic {
    override val physicsBody = Body(
        shape = shape,
        x = initialOffset.x,
        y = initialOffset.y,
    ).apply { density = 0f }
    override val body = PolygonBody(
        initialPosition = initialOffset,
        vertices = shape.vertices.map { SceneOffset(it.x, it.y) },
    )

    override fun update(deltaTimeInMillis: Float) {
        if (isRotating) {
            body.rotation -= (0.002 * deltaTimeInMillis).toFloat().rad
            physicsBody.orientation = body.rotation
        }
    }

    override fun DrawScope.draw() {
        val path = Path().apply {
            moveTo(body.vertices[0].x.raw + body.pivot.x.raw, body.vertices[0].y.raw + body.pivot.y.raw)
            for (i in 1 until body.vertices.size) {
                lineTo(body.vertices[i].x.raw + body.pivot.x.raw, body.vertices[i].y.raw + body.pivot.y.raw)
            }
            close()
        }
        drawPath(
            path = path,
            color = Color.DarkGray,
            style = Fill,
        )
        drawPath(
            path = path,
            color = Color.Black,
            style = Stroke(width = 2f),
        )
    }
}

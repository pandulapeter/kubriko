package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.PolygonBody
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.types.SceneOffset

internal class StaticPolygon(
    initialPosition: SceneOffset,
    shape: Polygon
) : RigidBody {
    override val physicsBody = Body(
        shape = shape,
        x = initialPosition.x,
        y = initialPosition.y,
    ).apply { density = 0f }
    override val body = PolygonBody(
        initialPosition = initialPosition,
        vertices = shape.vertices.map { SceneOffset(it.x, it.y) },
    )

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
            color = Color.LightGray,
            style = Fill,
        )
        drawPath(
            path = path,
            color = Color.Black,
            style = Stroke(width = 2f),
        )
    }
}

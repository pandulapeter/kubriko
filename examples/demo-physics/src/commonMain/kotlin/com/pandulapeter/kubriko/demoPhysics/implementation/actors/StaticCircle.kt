package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.body.PolygonBody
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Circle
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.types.SceneOffset

internal class StaticCircle(
    initialPosition: SceneOffset,
    shape: Circle
) : RigidBody {
    override val physicsBody = Body(
        shape = shape,
        x = initialPosition.x,
        y = initialPosition.y,
    ).apply { density = 0f }
    override val body = CircleBody(
        initialPosition = initialPosition,
        initialRadius = shape.radius,
    )
    private val radius = shape.radius

    override fun DrawScope.draw() {
        drawCircle(
            color = Color.LightGray,
            radius = radius.raw,
            center = body.size.center.raw,
        )
        drawCircle(
            color = Color.Black,
            radius = radius.raw,
            center = body.size.center.raw,
            style = Stroke(),
        )
        drawLine(
            color = Color.Black,
            start = Offset(0f, body.radius.raw),
            end = Offset(body.size.width.raw, body.radius.raw),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.Black,
            start = Offset(body.radius.raw, 0f),
            end = Offset(body.radius.raw, body.size.height.raw),
            strokeWidth = 2f,
        )
    }
}

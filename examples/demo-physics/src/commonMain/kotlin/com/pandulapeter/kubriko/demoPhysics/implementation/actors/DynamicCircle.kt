package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Circle
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

internal class DynamicCircle(
    initialOffset: SceneOffset,
    private val radius: SceneUnit,
) : BaseDynamicObject() {
    override val body = CircleBody(
        initialRadius = radius,
        initialPosition = initialOffset,
    )
    override val physicsBody = Body(
        shape = Circle(radius),
        x = initialOffset.x,
        y = initialOffset.y,
    ).apply {
        restitution = 0.5f
    }

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

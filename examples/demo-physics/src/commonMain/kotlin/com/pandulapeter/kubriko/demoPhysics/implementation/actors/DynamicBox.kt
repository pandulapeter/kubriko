package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class DynamicBox(
    initialOffset: SceneOffset,
    size: SceneSize,
) : BaseDynamicObject(
    shouldAutoRemove = true,
) {
    override val body = RectangleBody(
        initialSize = size,
        initialPosition = initialOffset,
    )
    override val physicsBody = Body(
        shape = Polygon(
            halfWidth = size.width / 2,
            halfHeight = size.height / 2,
        ),
        x = initialOffset.x,
        y = initialOffset.y,
    )

    override fun DrawScope.draw() {
        drawRect(
            color = Color.LightGray,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            size = body.size.raw,
            style = Stroke(),
        )
        drawLine(
            color = Color.Black,
            start = Offset.Zero,
            end = Offset(body.size.width.raw, body.size.height.raw),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.Black,
            start = Offset(body.size.width.raw, 0f),
            end = Offset(0f, body.size.height.raw),
            strokeWidth = 2f,
        )
    }
}

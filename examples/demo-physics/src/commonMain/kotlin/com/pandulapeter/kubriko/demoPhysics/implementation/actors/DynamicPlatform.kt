package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class DynamicPlatform(
    initialPosition: SceneOffset,
    size: SceneSize,
) : RigidBody, Dynamic {
    override val physicsBody = Body(
        shape = Polygon(size.width / 2f, size.height / 2f),
        x = initialPosition.x,
        y = initialPosition.y,
    ).apply { density = 0f }
    override val body = RectangleBody(
        initialPosition = initialPosition,
        initialSize = size,
    )

    override fun update(deltaTimeInMillis: Float) {
        body.rotation -= (0.002 * deltaTimeInMillis).toFloat().rad
        physicsBody.orientation = body.rotation
    }

    override fun DrawScope.draw() {
        drawRect(
            color = Color.DarkGray,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            size = body.size.raw,
            style = Stroke(),
        )
    }
}

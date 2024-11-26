package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class Platform(
    initialPosition: SceneOffset,
    boundingBox: SceneSize,
) : RigidBody, Dynamic {
    override val physicsBody = Body(
        Polygon(boundingBox.width.raw / 2f, boundingBox.height.raw / 2f),
        initialPosition.x.raw,
        initialPosition.y.raw,
    ).apply { density = 0f }
    override val body = RectangleBody(
        initialPosition = initialPosition,
        initialSize = boundingBox,
    )

    override fun update(deltaTimeInMillis: Float) {
        body.position = SceneOffset(physicsBody.position.x.scenePixel, physicsBody.position.y.scenePixel)
        body.rotation += (0.002 * deltaTimeInMillis).toFloat().rad
        physicsBody.orientation = body.rotation.normalized
    }

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
    }
}

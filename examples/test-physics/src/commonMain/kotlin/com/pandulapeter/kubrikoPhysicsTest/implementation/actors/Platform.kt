package com.pandulapeter.kubrikoPhysicsTest.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

class Platform(
    initialPosition: SceneOffset,
    override val boundingBox: SceneSize,
) : RigidBody, Dynamic {
    override val canvasIndex = -1
    override val body = Body(
        Polygon(boundingBox.width.raw / 2.0, boundingBox.height.raw / 2.0),
        initialPosition.x.raw.toDouble(),
        initialPosition.y.raw.toDouble(),
    ).apply { density = 0.0 }

    override fun update(deltaTimeInMillis: Float) {
        rotation += (0.002 * deltaTimeInMillis).toFloat().rad
    }

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = Color.White,
        size = boundingBox.raw,
    )
}

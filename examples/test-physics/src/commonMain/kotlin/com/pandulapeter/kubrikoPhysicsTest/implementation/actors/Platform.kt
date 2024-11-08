package com.pandulapeter.kubrikoPhysicsTest.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

class Platform(
    override var position: SceneOffset,
    override val boundingBox: SceneSize,
) : RigidBody, Dynamic {
    override val canvasIndex = -1
    override val body = Body(
        Polygon(boundingBox.width.raw / 2.0, boundingBox.height.raw / 2.0),
        position.x.raw.toDouble(),
        position.y.raw.toDouble(),
    ).apply { density = 0.0 }
    override var rotation = AngleRadians.Zero

    override fun update(deltaTimeInMillis: Float) {
        body.orientation += 0.002 * deltaTimeInMillis
    }

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = Color.White,
        size = boundingBox.raw,
    )
}

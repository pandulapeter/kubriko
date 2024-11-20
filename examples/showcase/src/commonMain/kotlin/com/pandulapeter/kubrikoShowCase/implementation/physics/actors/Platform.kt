package com.pandulapeter.kubrikoShowcase.implementation.physics.actors

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
    override val body = Body(
        Polygon(boundingBox.width.raw / 2f, boundingBox.height.raw / 2f),
        initialPosition.x.raw,
        initialPosition.y.raw,
    ).apply { density = 0f }

    override fun update(deltaTimeInMillis: Float) {
        rotation += (0.002 * deltaTimeInMillis).toFloat().rad
    }

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = Color.Gray,
        size = boundingBox.raw,
    )
}

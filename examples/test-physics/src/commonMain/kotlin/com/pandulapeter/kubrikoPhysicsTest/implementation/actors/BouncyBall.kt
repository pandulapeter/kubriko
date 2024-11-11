package com.pandulapeter.kubrikoPhysicsTest.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.wrapWithin
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Circle
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize

class BouncyBall(
    initialPosition: SceneOffset,
    private val radius: ScenePixel,
) : RigidBody, Dynamic {
    override val canvasIndex = -1
    override val boundingBox = SceneSize(radius, radius)
    override val body = Body(Circle(radius.raw.toDouble()), initialPosition.x.raw.toDouble(), initialPosition.y.raw.toDouble())
    private lateinit var viewportManager: ViewportManager

    override fun onAdd(kubriko: Kubriko) {
        viewportManager = kubriko.require()
    }

    override fun update(deltaTimeInMillis: Float) {
        position = position.wrapWithin(viewportManager.topLeft.value, viewportManager.bottomRight.value)
    }

    override fun draw(scope: DrawScope) {
        scope.drawCircle(
            color = Color.White,
            radius = radius.raw,
            center = pivotOffset.raw,
        )
        scope.drawLine(
            color = Color.Black,
            start = Offset(pivotOffset.raw.x - radius.raw, pivotOffset.raw.y),
            end = Offset(pivotOffset.raw.x + radius.raw, pivotOffset.raw.y),
            strokeWidth = 2f,
        )
        scope.drawLine(
            color = Color.Black,
            start = Offset(pivotOffset.raw.x, pivotOffset.raw.y - radius.raw),
            end = Offset(pivotOffset.raw.x, pivotOffset.raw.y + radius.raw),
            strokeWidth = 2f,
        )
    }
}

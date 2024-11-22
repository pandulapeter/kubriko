package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.wrapWithin
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Circle
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize

internal class BouncyBall(
    initialPosition: SceneOffset,
    private val radius: ScenePixel,
) : RigidBody, Dynamic {
    override val boundingBox = SceneSize(radius * 2, radius * 2)
    override val body = Body(Circle(radius.raw), initialPosition.x.raw, initialPosition.y.raw)
    private lateinit var viewportManager: ViewportManager

    override fun onAdd(kubriko: Kubriko) {
        viewportManager = kubriko.require()
    }

    override fun update(deltaTimeInMillis: Float) {
        position = wrapWithin(viewportManager.topLeft.value, viewportManager.bottomRight.value)
    }

    override fun DrawScope.draw() {
        drawCircle(
            color = Color.LightGray,
            radius = radius.raw,
            center = pivotOffset.raw,
        )
        drawCircle(
            color = Color.Black,
            radius = radius.raw,
            center = pivotOffset.raw,
            style = Stroke(),
        )
        drawLine(
            color = Color.Black,
            start = Offset(pivotOffset.raw.x - radius.raw, pivotOffset.raw.y),
            end = Offset(pivotOffset.raw.x + radius.raw, pivotOffset.raw.y),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.Black,
            start = Offset(pivotOffset.raw.x, pivotOffset.raw.y - radius.raw),
            end = Offset(pivotOffset.raw.x, pivotOffset.raw.y + radius.raw),
            strokeWidth = 2f,
        )
    }
}

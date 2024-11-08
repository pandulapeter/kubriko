package com.pandulapeter.kubrikoPhysicsTest.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.physicsManager.RigidBody
import com.pandulapeter.kubriko.physicsManager.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physicsManager.implementation.physics.geometry.Circle
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize

class BouncyBall(
    override var position: SceneOffset,
    private val radius: ScenePixel,
) : RigidBody {
    override val canvasIndex = -1
    override val boundingBox = SceneSize(radius, radius)
    override val body = Body(Circle(radius.raw.toDouble()), position.x.raw.toDouble(), position.y.raw.toDouble())
    override var rotation: AngleRadians = AngleRadians.Zero

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

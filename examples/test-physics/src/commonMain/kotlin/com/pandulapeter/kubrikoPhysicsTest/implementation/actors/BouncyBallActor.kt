package com.pandulapeter.kubrikoPhysicsTest.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.physicsManager.implementation.dynamics.Body
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

class BouncyBallActor(
    private val body: Body,
    private val radius: Float,
) : Visible, Dynamic {

    override val boundingBox: SceneSize = SceneSize(radius.scenePixel, radius.scenePixel)
    override var position: SceneOffset = body.position.let {
        SceneOffset(
            it.x.toFloat().scenePixel,
            it.y.toFloat().scenePixel,
        )
    }
    override var rotation: AngleRadians = AngleRadians.Zero

    override fun update(deltaTimeInMillis: Float) {
        position = body.position.let {
            SceneOffset(
                it.x.toFloat().scenePixel,
                it.y.toFloat().scenePixel,
            )
        }
        rotation = body.orientation.toFloat().rad
    }

    override fun draw(scope: DrawScope) {
        scope.drawCircle(
            color = Color.White,
            radius = radius,
            center = pivotOffset.raw,
        )
        scope.drawLine(
            color = Color.Black,
            start = Offset(pivotOffset.raw.x - radius, pivotOffset.raw.y),
            end = Offset(pivotOffset.raw.x + radius, pivotOffset.raw.y),
            strokeWidth = 2f,
        )
        scope.drawLine(
            color = Color.Black,
            start = Offset(pivotOffset.raw.x, pivotOffset.raw.y - radius),
            end = Offset(pivotOffset.raw.x, pivotOffset.raw.y + radius),
            strokeWidth = 2f,
        )
    }
}

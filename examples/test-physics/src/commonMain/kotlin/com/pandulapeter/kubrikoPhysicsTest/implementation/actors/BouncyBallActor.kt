package com.pandulapeter.kubrikoPhysicsTest.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.physicsManager.implementation.dynamics.Body
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

class BouncyBallActor(
    private val body: Body,
    radius: Float,
) : Visible, Dynamic {

    override val boundingBox: SceneSize = SceneSize(radius.scenePixel, radius.scenePixel)
    override var position: SceneOffset = body.position.let {
        SceneOffset(
            it.x.toFloat().scenePixel,
            it.y.toFloat().scenePixel,
        )
    }

    override fun update(deltaTimeInMillis: Float) {
        position = body.position.let {
            SceneOffset(
                it.x.toFloat().scenePixel,
                it.y.toFloat().scenePixel,
            )
        }
    }

    override fun draw(scope: DrawScope) = scope.drawCircle(
        color = Color.White,
        radius = (boundingBox.raw.width + boundingBox.raw.height) / 2,
        center = pivotOffset.raw,
    )
}

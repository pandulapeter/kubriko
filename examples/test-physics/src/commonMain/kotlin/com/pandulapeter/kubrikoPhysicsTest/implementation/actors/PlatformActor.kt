package com.pandulapeter.kubrikoPhysicsTest.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.physicsManager.implementation.dynamics.Body
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

class PlatformActor(
    body: Body,
) : Visible {

    override val canvasIndex = -1
    override val boundingBox: SceneSize = SceneSize(1200f.scenePixel, 40f.scenePixel)
    override var position: SceneOffset = body.position.let {
        SceneOffset(
            it.x.toFloat().scenePixel,
            it.y.toFloat().scenePixel,
        )
    }

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = Color.White,
        size = boundingBox.raw,
    )
}

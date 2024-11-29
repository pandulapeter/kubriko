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

internal class ChainLink(
    initialPosition: SceneOffset,
) : RigidBody, Dynamic {
    override val physicsBody = Body(
        shape = Polygon(Width.raw / 2f, Height.raw / 2f),
        x = initialPosition.x.raw,
        y = initialPosition.y.raw,
    )
    override val body = RectangleBody(
        initialPosition = initialPosition,
        initialSize = SceneSize(Width, Height),
    )

    override fun update(deltaTimeInMillis: Float) {
        body.position = SceneOffset(physicsBody.position.x.scenePixel, physicsBody.position.y.scenePixel)
        body.rotation = physicsBody.orientation.rad
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

    companion object {
        private val Width = 40f.scenePixel
        private val Height = 10f.scenePixel
    }
}

package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.implementation.extensions.wrapWithin
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Circle
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize

internal class BouncyBall(
    initialPosition: SceneOffset,
    private val radius: ScenePixel,
) : RigidBody, Dynamic {
    override val body = CircleBody(
        initialRadius = radius,
        initialPosition = initialPosition,
    )
    override val physicsBody = Body(Circle(radius.raw), initialPosition.x.raw, initialPosition.y.raw)
    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        viewportManager = kubriko.require()
    }

    override fun update(deltaTimeInMillis: Float) {
        body.position = SceneOffset(physicsBody.position.x.scenePixel, physicsBody.position.y.scenePixel)
        physicsBody.position = wrapWithin(viewportManager.topLeft.value, viewportManager.bottomRight.value).let { Vec2(it.x.raw, it.y.raw) }
        body.position = SceneOffset(physicsBody.position.x.scenePixel, physicsBody.position.y.scenePixel)
        body.rotation = physicsBody.orientation.rad
    }

    override fun DrawScope.draw() {
        drawCircle(
            color = Color.LightGray,
            radius = radius.raw,
            center = body.size.center.raw,
        )
        drawCircle(
            color = Color.Black,
            radius = radius.raw,
            center = body.size.center.raw,
            style = Stroke(),
        )
        drawLine(
            color = Color.Black,
            start = Offset(0f, body.radius.raw),
            end = Offset(body.size.width.raw, body.radius.raw),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.Black,
            start = Offset(body.radius.raw, 0f),
            end = Offset(body.radius.raw, body.size.height.raw),
            strokeWidth = 2f,
        )
    }
}

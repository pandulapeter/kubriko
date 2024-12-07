package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.implementation.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.JointWrapper
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.physics.implementation.physics.joints.JointToBody
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit

internal class DynamicChain(
    initialCenterOffset: SceneOffset
) : Group, Dynamic {
    private val chainLinks = (0..LINK_COUNT).map { linkIndex ->
        ChainLink(
            initialPosition = SceneOffset(
                x = initialCenterOffset.x + LinkDistance * (LINK_COUNT / 2) - (LinkDistance * linkIndex),
                y = initialCenterOffset.y,
            )
        )
    }
    private val joints = chainLinks.mapIndexedNotNull { index, chainLink ->
        if (index > 0) JointWrapper(
            physicsJoint = JointToBody(
                body1 = chainLinks[index - 1].physicsBody,
                body2 = chainLink.physicsBody,
                jointLength = 10f.sceneUnit,
                jointConstant = 200f,
                dampening = 10f,
                canGoSlack = true,
                offset1 = Vec2((-10).sceneUnit, SceneUnit.Zero),
                offset2 = Vec2(10.sceneUnit, SceneUnit.Zero),
            )
        ) else null
    }
    private lateinit var actorManager: ActorManager
    private lateinit var viewportManager: ViewportManager
    override val actors = chainLinks + joints

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.require()
        viewportManager = kubriko.require()
    }

    override fun update(deltaTimeInMillis: Float) {
        if (chainLinks.none { it.body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager) }) {
            actorManager.remove(this)
        }
    }

    private class ChainLink(
        initialPosition: SceneOffset,
    ) : BaseDynamicObject(
        shouldAutoRemove = false,
    ) {
        override val physicsBody = Body(
            shape = Polygon(Width / 2f, Height / 2f),
            x = initialPosition.x,
            y = initialPosition.y,
        )
        override val body = RectangleBody(
            initialPosition = initialPosition,
            initialSize = SceneSize(Width, Height),
        )

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
            private val Width = 40f.sceneUnit
            private val Height = 10f.sceneUnit
        }
    }

    companion object {
        private const val LINK_COUNT = 20
        private val LinkDistance = 40.sceneUnit
    }
}
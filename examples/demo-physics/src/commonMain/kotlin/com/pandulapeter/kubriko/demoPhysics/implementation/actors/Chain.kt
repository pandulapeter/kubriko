package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.implementation.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.JointWrapper
import com.pandulapeter.kubriko.physics.implementation.physics.joints.JointToBody
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.SceneOffset

internal class Chain(
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
                jointLength = 10f,
                jointConstant = 200f,
                dampening = 10f,
                canGoSlack = true,
                offset1 = Vec2(-10f, 0f),
                offset2 = Vec2(10f, 0f),
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

    companion object {
        private const val LINK_COUNT = 20
        private val LinkDistance = 40.sceneUnit
    }
}
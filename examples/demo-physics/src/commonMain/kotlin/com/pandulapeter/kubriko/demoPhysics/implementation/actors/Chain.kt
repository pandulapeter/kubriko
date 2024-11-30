package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.physics.JointWrapper
import com.pandulapeter.kubriko.physics.implementation.physics.joints.JointToBody
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.SceneOffset

internal class Chain(
    initialCenterOffset: SceneOffset
) : Group {
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
    override val actors = chainLinks + joints

    companion object {
        private const val LINK_COUNT = 20
        private val LinkDistance = 40.scenePixel
    }
}
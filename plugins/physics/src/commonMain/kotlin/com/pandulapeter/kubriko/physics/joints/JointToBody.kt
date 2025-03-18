/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.joints

import com.pandulapeter.kubriko.collision.implementation.Mat2
import com.pandulapeter.kubriko.helpers.extensions.cross
import com.pandulapeter.kubriko.helpers.extensions.dot
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.normalized
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Class for a joint between two bodies.
 */
class JointToBody
/**
 * Constructor for a joint between two bodies.
 *
 * @param physicsBody1            First body the joint is attached to
 * @param physicsBody2            Second body the joint is attached to
 * @param jointLength   The desired distance of the joint between two points/bodies
 * @param jointConstant The strength of the joint
 * @param dampening     The dampening constant to use for the joints forces
 * @param canGoSlack    Boolean whether the joint can go slack or not
 * @param offset1       Offset to be applied to the location of the joint relative to b1's object space
 * @param offset2       Offset to be applied to the location of the joint relative to b2's object space
 */(
    physicsBody1: PhysicsBody,
    private val physicsBody2: PhysicsBody,
    jointLength: SceneUnit,
    jointConstant: Float,
    dampening: Float,
    canGoSlack: Boolean,
    offset1: SceneOffset,
    private val offset2: SceneOffset
) : Joint(physicsBody1, jointLength, jointConstant, dampening, canGoSlack, offset1) {
    private var object2AttachmentPoint = physicsBody2.position + Mat2(physicsBody2.orientation).times(offset2)

    /**
     * Applies tension to the two bodies.
     */
    override fun applyTension() {
        val mat1 = Mat2(physicsBody.orientation)
        object1AttachmentPoint = physicsBody.position + mat1.times(offset)
        val mat2 = Mat2(physicsBody2.orientation)
        object2AttachmentPoint = physicsBody2.position + mat2.times(offset2)
        val tension = calculateTension()
        val distance = object2AttachmentPoint.minus(object1AttachmentPoint).normalized()
        val impulse = distance.scalar(tension)
        physicsBody.applyLinearImpulse(impulse, object1AttachmentPoint - physicsBody.position)
        physicsBody2.applyLinearImpulse(-impulse, object2AttachmentPoint - physicsBody2.position)
    }

    /**
     * Calculates tension between the two attachment points of the joints bodies.
     *
     * @return double value of the tension force between the two bodies attachment points
     */
    override fun calculateTension(): SceneUnit {
        val distance = object1AttachmentPoint.minus(object2AttachmentPoint).length()
        if (distance < naturalLength && canGoSlack) {
            return SceneUnit.Zero
        }
        val extensionRatio = distance - naturalLength
        val tensionDueToHooksLaw = extensionRatio * springConstant
        val tensionDueToMotionDamping = rateOfChangeOfExtension() * dampeningConstant
        return tensionDueToHooksLaw + tensionDueToMotionDamping
    }

    /**
     * Determines the rate of change between two objects.
     *
     * @return double value of the rate of change
     */
    override fun rateOfChangeOfExtension(): SceneUnit {
        val distance = object2AttachmentPoint.minus(object1AttachmentPoint)
        distance.normalized()
        val relativeVelocity = physicsBody2.velocity.plus(
            object2AttachmentPoint.minus(physicsBody2.position).cross(physicsBody2.angularVelocity.raw)
        ).minus(physicsBody.velocity).minus(
            object1AttachmentPoint.minus(physicsBody.position).cross(physicsBody.angularVelocity.raw)
        )
        return relativeVelocity.dot(distance)
    }
}
/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.joints

import com.pandulapeter.kubriko.collision.implementation.RotationMatrix
import com.pandulapeter.kubriko.helpers.extensions.cross
import com.pandulapeter.kubriko.helpers.extensions.dot
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.normalized
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

class JointToBody(
    physicsBody1: PhysicsBody,
    private val physicsBody2: PhysicsBody,
    jointLength: SceneUnit,
    jointConstant: Float,
    dampening: Float,
    canGoSlack: Boolean,
    offset1: SceneOffset,
    private val offset2: SceneOffset
) : Joint(physicsBody1, jointLength, jointConstant, dampening, canGoSlack, offset1) {
    private var object2AttachmentPoint = physicsBody2.position + RotationMatrix(physicsBody2.rotation).times(offset2)

    override fun applyTension() {
        val mat1 = RotationMatrix(physicsBody.rotation)
        object1AttachmentPoint = physicsBody.position + mat1.times(offset)
        val rotationMatrix = RotationMatrix(physicsBody2.rotation)
        object2AttachmentPoint = physicsBody2.position + rotationMatrix.times(offset2)
        val tension = calculateTension()
        val distance = object2AttachmentPoint.minus(object1AttachmentPoint).normalized()
        val impulse = distance.scalar(tension)
        physicsBody.applyLinearImpulse(impulse, object1AttachmentPoint - physicsBody.position)
        physicsBody2.applyLinearImpulse(-impulse, object2AttachmentPoint - physicsBody2.position)
    }

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

    override fun rateOfChangeOfExtension() = physicsBody2.velocity
        .plus(object2AttachmentPoint.minus(physicsBody2.position).cross(physicsBody2.angularVelocity.raw))
        .minus(physicsBody.velocity)
        .minus(object1AttachmentPoint.minus(physicsBody.position).cross(physicsBody.angularVelocity.raw))
        .dot((object2AttachmentPoint - object1AttachmentPoint).normalized())
}
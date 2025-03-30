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

import com.pandulapeter.kubriko.collision.implementation.RotationMatrix
import com.pandulapeter.kubriko.helpers.extensions.cross
import com.pandulapeter.kubriko.helpers.extensions.dot
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.normalized
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

class JointToPoint(
    b1: PhysicsBody,
    val pointAttachedTo: SceneOffset,
    jointLength: SceneUnit,
    jointConstant: Float,
    dampening: Float,
    canGoSlack: Boolean,
    offset: SceneOffset
) : Joint(b1, jointLength, jointConstant, dampening, canGoSlack, offset) {

    override fun applyTension() {
        val mat1 = RotationMatrix(physicsBody.rotation)
        object1AttachmentPoint = physicsBody.position + mat1.times(offset)
        val tension = calculateTension()
        val distance = pointAttachedTo.minus(object1AttachmentPoint).normalized()
        val impulse = distance.scalar(tension)
        physicsBody.applyLinearImpulse(impulse, object1AttachmentPoint.minus(physicsBody.position))
    }

    override fun calculateTension(): SceneUnit {
        val distance = object1AttachmentPoint.minus(pointAttachedTo).length()
        if (distance < naturalLength && canGoSlack) {
            return SceneUnit.Zero
        }
        val extensionRatio = distance - naturalLength
        val tensionDueToHooksLaw = extensionRatio * springConstant
        val tensionDueToMotionDamping = rateOfChangeOfExtension() * dampeningConstant
        return tensionDueToHooksLaw + tensionDueToMotionDamping
    }

    override fun rateOfChangeOfExtension() = -physicsBody.velocity
        .minus(object1AttachmentPoint.minus(physicsBody.position).cross(physicsBody.angularVelocity.raw))
        .dot((pointAttachedTo - object1AttachmentPoint).normalized())
}
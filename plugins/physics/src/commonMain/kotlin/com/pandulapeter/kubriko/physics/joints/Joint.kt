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
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Abstract class for joints holding all the common properties of joints.
 *
 * @param physicsBody            A body the joint is attached to
 * @param naturalLength   The desired distance of the joint between two points/bodies
 * @param springConstant The strength of the joint
 * @param dampeningConstant     The dampening constant to use for the joints forces
 * @param canGoSlack    Boolean whether the joint can go slack or not
 * @param offset       Offset to be applied to the location of the joint relative to b1's object space.
 */
sealed class Joint protected constructor(
    protected val physicsBody: PhysicsBody,
    protected val naturalLength: SceneUnit,
    protected val springConstant: Float,
    protected val dampeningConstant: Float,
    protected val canGoSlack: Boolean,
    protected val offset: SceneOffset
) {
    var object1AttachmentPoint = physicsBody.position + RotationMatrix(physicsBody.rotation).times(offset)

    /**
     * Abstract method to apply tension to the joint
     */
    abstract fun applyTension()

    /**
     * Abstract method to calculate tension between the joint
     *
     * @return value of the tension force between two points/bodies
     */
    abstract fun calculateTension(): SceneUnit

    /**
     * Determines the rate of change between two objects/points.
     * @return value of the rate of change
     */
    abstract fun rateOfChangeOfExtension(): SceneUnit
}
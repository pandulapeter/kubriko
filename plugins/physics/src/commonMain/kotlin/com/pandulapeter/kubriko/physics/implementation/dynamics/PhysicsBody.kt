/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation.dynamics

import com.pandulapeter.kubriko.physics.implementation.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physics.implementation.collision.bodies.CollisionBodyInterface
import com.pandulapeter.kubriko.physics.implementation.dynamics.bodies.AbstractPhysicalBody
import com.pandulapeter.kubriko.physics.implementation.geometry.Shape
import com.pandulapeter.kubriko.physics.implementation.math.Vec2
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Class to create a body to add to a world.
 *
 * @param shape Shape to bind to body.
 * @param x     Position x in world space.
 * @param y     Position y in world space.
 */
// TODO: Should inherit from BoxBody?
class PhysicsBody(
    override var shape: Shape,
    x: SceneUnit,
    y: SceneUnit,
) : AbstractPhysicalBody(), CollisionBodyInterface {
    override var position: Vec2 = Vec2(x, y)
    override var dynamicFriction = 0.2f
    override var staticFriction = 0.5f
    override var orientation = AngleRadians.Zero
        set(value) {
            field = value
            shape.orientation.set(orientation)
            shape.createAABB()
        }
    override lateinit var aabb: AxisAlignedBoundingBox

    init {
        density = density
        shape.body = this
        shape.orientation.set(orientation)
        shape.createAABB()
    }
}
package com.pandulapeter.kubriko.physics.implementation.physics.dynamics

import com.pandulapeter.kubriko.physics.implementation.physics.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physics.implementation.physics.collision.bodies.CollisionBodyInterface
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.bodies.AbstractPhysicalBody
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Shape
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Class to create a body to add to a world.
 *
 * @param shape Shape to bind to body.
 * @param x     Position x in world space.
 * @param y     Position y in world space.
 */
// TODO: Should inherit from RectangleBody?
class Body(override var shape: Shape, x: SceneUnit, y: SceneUnit) : AbstractPhysicalBody(), CollisionBodyInterface {
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
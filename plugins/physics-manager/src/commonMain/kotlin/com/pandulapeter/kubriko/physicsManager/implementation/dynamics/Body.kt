package com.pandulapeter.kubriko.physicsManager.implementation.dynamics

import com.pandulapeter.kubriko.physicsManager.implementation.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physicsManager.implementation.collision.bodies.CollisionBodyInterface
import com.pandulapeter.kubriko.physicsManager.implementation.dynamics.bodies.AbstractPhysicalBody
import com.pandulapeter.kubriko.physicsManager.implementation.geometry.Shape
import com.pandulapeter.kubriko.physicsManager.implementation.math.Vec2

/**
 * Class to create a body to add to a world.
 *
 * @param shape Shape to bind to body.
 * @param x     Position x in world space.
 * @param y     Position y in world space.
 */
class Body(override var shape: Shape, x: Double, y: Double) : AbstractPhysicalBody(), CollisionBodyInterface {
    override var position: Vec2 = Vec2(x, y)
    override var dynamicFriction = .2
    override var staticFriction = .5
    override var orientation = .0
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
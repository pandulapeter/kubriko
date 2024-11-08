package com.pandulapeter.kubriko.physicsManager.implementation.collision.bodies

import com.pandulapeter.kubriko.physicsManager.implementation.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physicsManager.implementation.geometry.Shape
import com.pandulapeter.kubriko.physicsManager.implementation.math.Vec2

class CollisionBody(override var shape: Shape, x: Double, y: Double) : CollisionBodyInterface {
    override var position: Vec2 = Vec2(x, y)
    override var dynamicFriction = .5
    override var staticFriction = .2
    override var orientation = .0
        set(value) {
            field = value
            shape.orientation.set(orientation)
            shape.createAABB()
        }
    override lateinit var aabb: AxisAlignedBoundingBox

    init {
        shape.body = this
        shape.orientation.set(orientation)
        shape.createAABB()
    }
}
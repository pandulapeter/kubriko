package com.pandulapeter.kubriko.physics.implementation.collision.bodies

import com.pandulapeter.kubriko.physics.implementation.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physics.implementation.geometry.Shape
import com.pandulapeter.kubriko.physics.implementation.math.Vec2
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit

class CollisionBody(override var shape: Shape, x: SceneUnit, y: SceneUnit) : CollisionBodyInterface {
    override var position: Vec2 = Vec2(x, y)
    override var dynamicFriction = 0.5f
    override var staticFriction = 0.2f
    override var orientation = AngleRadians.Zero
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
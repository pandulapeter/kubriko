package com.pandulapeter.kubriko.physics.implementation.physics.collision.bodies

import com.pandulapeter.kubriko.physics.implementation.physics.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Shape
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.bodies.TranslatableBody

interface CollisionBodyInterface : TranslatableBody {
    var shape: Shape
    var dynamicFriction: Double
    var staticFriction: Double
    var orientation: Double
    var aabb: AxisAlignedBoundingBox
}
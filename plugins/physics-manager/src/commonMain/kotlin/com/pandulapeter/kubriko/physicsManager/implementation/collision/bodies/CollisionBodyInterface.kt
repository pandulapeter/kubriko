package com.pandulapeter.kubriko.physicsManager.implementation.collision.bodies

import com.pandulapeter.kubriko.physicsManager.implementation.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physicsManager.implementation.geometry.Shape
import com.pandulapeter.kubriko.physicsManager.implementation.geometry.bodies.TranslatableBody

interface CollisionBodyInterface : TranslatableBody {
    var shape: Shape
    var dynamicFriction: Double
    var staticFriction: Double
    var orientation: Double
    var aabb: AxisAlignedBoundingBox
}
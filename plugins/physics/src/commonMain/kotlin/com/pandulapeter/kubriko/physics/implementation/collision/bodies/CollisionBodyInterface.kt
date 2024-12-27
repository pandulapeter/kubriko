package com.pandulapeter.kubriko.physics.implementation.collision.bodies

import com.pandulapeter.kubriko.physics.implementation.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physics.implementation.geometry.Shape
import com.pandulapeter.kubriko.physics.implementation.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.types.AngleRadians

interface CollisionBodyInterface : TranslatableBody {
    var shape: Shape
    var dynamicFriction: Float
    var staticFriction: Float
    var orientation: AngleRadians
    var aabb: AxisAlignedBoundingBox
}
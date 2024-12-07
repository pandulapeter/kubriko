package com.pandulapeter.kubriko.physics.implementation.physics.collision.bodies

import com.pandulapeter.kubriko.physics.implementation.physics.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Shape
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.types.AngleRadians

interface CollisionBodyInterface : TranslatableBody {
    var shape: Shape
    var dynamicFriction: Float
    var staticFriction: Float
    var orientation: AngleRadians
    var aabb: AxisAlignedBoundingBox
}
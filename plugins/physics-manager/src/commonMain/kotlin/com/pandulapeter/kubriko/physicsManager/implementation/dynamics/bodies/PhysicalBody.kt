package com.pandulapeter.kubriko.physicsManager.implementation.dynamics.bodies

import com.pandulapeter.kubriko.physicsManager.implementation.math.Vec2

class PhysicalBody(x: Double, y: Double) :
    AbstractPhysicalBody() {
    override var position = Vec2(x, y)

    init {
        density = density
    }
}
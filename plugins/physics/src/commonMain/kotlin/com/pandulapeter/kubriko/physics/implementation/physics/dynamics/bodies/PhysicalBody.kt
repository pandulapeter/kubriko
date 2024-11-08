package com.pandulapeter.kubriko.physics.implementation.physics.dynamics.bodies

import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2

class PhysicalBody(x: Double, y: Double) :
    AbstractPhysicalBody() {
    override var position = Vec2(x, y)

    init {
        density = density
    }
}
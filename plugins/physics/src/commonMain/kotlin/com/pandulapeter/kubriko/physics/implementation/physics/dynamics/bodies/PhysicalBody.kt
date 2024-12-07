package com.pandulapeter.kubriko.physics.implementation.physics.dynamics.bodies

import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.SceneUnit

class PhysicalBody(x: SceneUnit, y: SceneUnit) :
    AbstractPhysicalBody() {
    override var position = Vec2(x, y)

    init {
        density = density
    }
}
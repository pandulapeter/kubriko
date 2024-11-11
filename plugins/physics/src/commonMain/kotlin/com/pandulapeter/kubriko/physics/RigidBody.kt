package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset

interface RigidBody : Visible, Actor {

    val body: Body
    override var rotation: AngleRadians
        get() = body.orientation.toFloat().rad
        set(value) {
            body.orientation = value.normalized.toDouble()
        }
    override var position: SceneOffset
        get() = body.position.let { SceneOffset(it.x.toFloat().scenePixel, it.y.toFloat().scenePixel) }
        set(value) {
            body.position = Vec2(value.x.raw.toDouble(), value.y.raw.toDouble())
        }
}
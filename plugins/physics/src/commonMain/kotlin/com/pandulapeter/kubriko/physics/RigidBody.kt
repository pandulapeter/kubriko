package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body

interface RigidBody : Visible, Collidable {

    // TODO Should be merged with body
    val physicsBody: Body
}
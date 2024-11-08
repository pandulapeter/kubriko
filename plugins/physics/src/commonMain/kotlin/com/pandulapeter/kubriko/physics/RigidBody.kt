package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.types.AngleRadians

interface RigidBody : Visible, Actor {

    override var rotation: AngleRadians
    val body: Body
}
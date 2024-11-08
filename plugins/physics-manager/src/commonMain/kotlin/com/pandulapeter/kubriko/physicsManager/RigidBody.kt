package com.pandulapeter.kubriko.physicsManager

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.physicsManager.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.types.AngleRadians

interface RigidBody : Visible, Actor {

    override var rotation: AngleRadians
    val body: Body
}
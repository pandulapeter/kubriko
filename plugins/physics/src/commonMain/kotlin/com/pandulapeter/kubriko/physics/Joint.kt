package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.physics.implementation.joints.Joint

data class JointWrapper(
    val physicsJoint: Joint,
) : Actor
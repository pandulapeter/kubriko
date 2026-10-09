/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.physics.joints.Joint

/**
 * An Actor that hands [physicsJoint] to the `PhysicsManager` of the Kubriko instance it is added to.
 * Add it alongside the bodies the joint connects.
 */
interface JointWrapper : Actor {

    /**
     * The joint simulated while this Actor is in the scene.
     */
    val physicsJoint: Joint
}
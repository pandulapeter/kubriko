/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision

import kotlin.reflect.KClass

/**
 * A specialized [Collidable] that can react to collisions with other collidable actors.
 *
 * Actors implementing this trait will receive callbacks when they overlap with other [Collidable]s
 * that match the specified [collidableTypes].
 */
interface CollisionDetector : Collidable {

    /**
     * The types of [Collidable] actors that this detector should listen for.
     */
    val collidableTypes: List<KClass<out Collidable>>

    /**
     * Called when a collision is detected between this object and one or more other [Collidable]s.
     *
     * @param collidables The list of objects that were found to be colliding with this detector.
     */
    fun onCollisionDetected(collidables: List<Collidable>)
}
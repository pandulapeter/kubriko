/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.rays

import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Ray information class to store relevant data about rays and any intersection found.
 */
internal class RayInformation {
    /**
     * Getter for body variable.
     *
     * @return returns b variable of type Body.
     */
    val body: PhysicsBody

    /**
     * Getter for coords variable.
     *
     * @return returns coords variable of type Vec2.
     */
    val coordinates: SceneOffset

    /**
     * Getter for index variable.
     *
     * @return returns index variable of type int.
     */
    // Poly index is the first index of the line of intersection found
    val index: Int

    /**
     * Constructor to store information about a ray intersection.
     *
     * @param b     Body involved with ray intersection.
     * @param x     x position of intersection.
     * @param y     y position of intersection.
     * @param index Index of shapes side that intersection intersects.
     */
    constructor(body: PhysicsBody, x: SceneUnit, y: SceneUnit, index: Int) {
        this.body = body
        coordinates = SceneOffset(x, y)
        this.index = index
    }

    /**
     * Convenience constructor equivalent to
     * [.RayInformation]
     *
     * @param b     Body involved with ray intersection.
     * @param sceneOffset     x/y position of intersection.
     * @param index Index of shapes side that intersection intersects.
     */
    constructor(body: PhysicsBody, sceneOffset: SceneOffset, index: Int) {
        this.body = body
        coordinates = sceneOffset
        this.index = index
    }
}
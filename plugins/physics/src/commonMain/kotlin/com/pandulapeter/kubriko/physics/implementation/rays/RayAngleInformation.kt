/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation.rays

/**
 * Ray information class to store relevant data about rays and any intersection found specific to shadow casting.
 */
internal class RayAngleInformation
/**
 * Constructor to store ray information.
 *
 * @param ray   Ray of intersection.
 * @param angle Angle the ray is set to.
 */(
    /**
     * Getter for RAY.
     *
     * @return returns RAY.
     */
    val ray: Ray,
    /**
     * Getter for ANGLE.
     *
     * @return returns ANGLE.
     */
    val angle: Double
)
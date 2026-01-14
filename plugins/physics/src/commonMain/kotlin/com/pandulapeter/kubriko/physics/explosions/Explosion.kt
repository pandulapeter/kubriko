/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.explosions

import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Interface detailing what explosions need to include.
 */
interface Explosion {

    /**
     * Applies a blast impulse to the effected bodies.
     *
     * @param blastPower The impulse magnitude.
     */
    fun applyBlastImpulse(blastPower: SceneUnit)

    /**
     * Updates the list of objects that should be effected by the explosion.
     */
    fun update(bodiesToEvaluate: Collection<PhysicsBody>)

    /**
     * Sets the epicenter of the explosion to a different coordinate.
     */
    fun setEpicenter(epicenter: SceneOffset)
}
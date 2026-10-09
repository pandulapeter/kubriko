/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.slingshot

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals

class CameraFollowTest {

    @Test
    fun sixtyHertzTickClosesTwoAndAHalfPercent() {
        assertEquals(0.025f, cameraFollowFactor(17), 0.001f)
    }

    @Test
    fun twoShortTicksMatchOneLongTick() {
        assertEquals(cameraFollowFactor(16), 1f - (1f - cameraFollowFactor(8)).pow(2), 1e-6f)
        assertEquals(cameraFollowFactor(33), 1f - (1f - cameraFollowFactor(11)).pow(3), 1e-6f)
    }

    @Test
    fun zeroDeltaDoesNotMoveTheCamera() {
        assertEquals(0f, cameraFollowFactor(0))
    }
}

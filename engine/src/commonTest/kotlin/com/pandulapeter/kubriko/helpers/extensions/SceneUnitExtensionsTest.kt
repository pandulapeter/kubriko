/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

class SceneUnitExtensionsTest {

    @Test
    fun clampBoundsEachGivenSide() {
        assertEquals(10f, 50f.sceneUnit.clamp(max = 10f.sceneUnit).raw)
        assertEquals(-3f, (-3f).sceneUnit.clamp(max = 10f.sceneUnit).raw)
        assertEquals(0f, (-5f).sceneUnit.clamp(min = 0f.sceneUnit).raw)
        assertEquals(7f, 7f.sceneUnit.clamp(min = 0f.sceneUnit).raw)
        assertEquals(4f, 4f.sceneUnit.clamp().raw)
        assertEquals(10f, 50f.sceneUnit.clamp(min = 0f.sceneUnit, max = 10f.sceneUnit).raw)
        assertEquals(0f, (-50f).sceneUnit.clamp(min = 0f.sceneUnit, max = 10f.sceneUnit).raw)
    }

    @Test
    fun invertedBoundsLetTheMinimumWin() {
        assertEquals(5f, 7f.sceneUnit.clamp(min = 5f.sceneUnit, max = 1f.sceneUnit).raw)
    }

    @Test
    fun lerpInterpolatesLinearly() {
        assertEquals(2f, lerp(2f.sceneUnit, 10f.sceneUnit, 0f).raw)
        assertEquals(6f, lerp(2f.sceneUnit, 10f.sceneUnit, 0.5f).raw)
        assertEquals(10f, lerp(2f.sceneUnit, 10f.sceneUnit, 1f).raw)
        assertEquals(14f, lerp(2f.sceneUnit, 10f.sceneUnit, 1.5f).raw)
    }

    @Test
    fun lerpBetweenEqualEndsGivesTheValueBackExactly() {
        val value = 0.1f.sceneUnit

        for (fraction in listOf(0f, 0.3f, 0.7f, 1f)) {
            assertEquals(value, lerp(value, value, fraction))
        }
    }
}

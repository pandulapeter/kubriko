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
}

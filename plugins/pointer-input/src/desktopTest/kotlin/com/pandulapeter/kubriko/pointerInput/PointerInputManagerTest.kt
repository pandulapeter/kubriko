/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.pointerInput

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pointer events only reach the manager through the Compose pointer input of a composed viewport, which library test
 * classpaths cannot host (no Skia runtime), so these tests are limited to the contracts that hold without one.
 */
class PointerInputManagerTest {

    private val pointerInputManager = PointerInputManager.newInstance()
    private val manualKubriko = newManualKubriko(pointerInputManager)

    @AfterTest
    fun tearDown() = manualKubriko.dispose()

    @Test
    fun noPointerIsPressedOrHoveringBeforeAnyInput() {
        manualKubriko.tick()

        assertTrue(pointerInputManager.pressedPointerPositions.value.isEmpty())
        assertNull(pointerInputManager.hoveringPointerPosition.value)
    }

    @Test
    fun movingThePointerFailsWithoutAWindow() {
        assertFalse(pointerInputManager.tryToMoveHoveringPointer(Offset(10f, 10f)))
    }
}

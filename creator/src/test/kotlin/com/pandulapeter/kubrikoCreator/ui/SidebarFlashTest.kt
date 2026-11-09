/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class SidebarFlashTest {

    @Test
    fun onlyTheRowThatChangedLightsUp() {
        val flash = SidebarFlash(name = "Ball", key = 3)
        assertEquals(3, flash.keyFor("Ball"))
        assertEquals(null, flash.keyFor("Bat"))
        assertEquals(null, SidebarFlash().keyFor("Ball"))
    }
}

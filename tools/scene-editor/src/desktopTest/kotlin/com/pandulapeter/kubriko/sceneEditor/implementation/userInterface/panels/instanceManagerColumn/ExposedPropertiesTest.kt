/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn

import com.pandulapeter.kubriko.sceneEditor.Exposed
import kotlin.test.Test
import kotlin.test.assertEquals

class ExposedPropertiesTest {

    private class Fixture {
        var zeta: Int = 0
            @Exposed(name = "Zeta") set
        var alpha: Float = 0f
            @Exposed(name = "Alpha") set
        var notExposed: Int = 0
        val readOnly: Int = 1
    }

    @Test
    fun onlyExposedMutablePropertiesAreListedSortedByName() {
        assertEquals(listOf("alpha", "zeta"), exposedMutableProperties(Fixture::class).map { it.name })
    }
}

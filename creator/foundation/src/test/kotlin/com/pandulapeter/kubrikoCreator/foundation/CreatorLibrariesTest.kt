/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import kotlin.test.Test
import kotlin.test.assertEquals

class CreatorLibrariesTest {

    @Test
    fun linesAreReadAsModulesAndVersions() {
        assertEquals(
            mapOf("org.jetbrains.kotlin:kotlin-stdlib" to "2.4.21", "org.jetbrains.compose.runtime:runtime-desktop" to "1.12.2"),
            CreatorLibraries.parse("org.jetbrains.kotlin:kotlin-stdlib=2.4.21\n\norg.jetbrains.compose.runtime:runtime-desktop=1.12.2\nno version\n"),
        )
    }
}

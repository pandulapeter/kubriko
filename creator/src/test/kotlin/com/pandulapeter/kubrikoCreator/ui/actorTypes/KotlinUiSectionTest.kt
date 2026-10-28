/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KotlinUiSectionTest {

    @Test
    fun thePreviewShowsTheFirstStatementsWithoutTheImports() {
        assertEquals("Column {\n}", kotlinUiPreview("import a.B\n\nColumn {\n}\n"))
        assertEquals((1..6).joinToString("\n") + "\n…", kotlinUiPreview((1..8).joinToString("\n")))
        assertNull(kotlinUiPreview("import a.B"))
        assertNull(kotlinUiPreview(null))
    }
}

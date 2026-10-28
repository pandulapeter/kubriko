/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KotlinUiTest {

    @Test
    fun leadingImportsAreSplitFromTheStatements() {
        val ui = KotlinUi.of("\nimport androidx.compose.foundation.layout.Column\n\n  import a.b as c  \nColumn {\n\n    importantText()\n}\n\n")
        assertEquals(listOf("androidx.compose.foundation.layout.Column", "a.b as c"), ui?.imports)
        assertEquals(listOf("Column {", "", "    importantText()", "}"), ui?.statements)
    }

    @Test
    fun codeWithoutStatementsIsNoUi() {
        assertNull(KotlinUi.of(null))
        assertNull(KotlinUi.of(" \n "))
        assertNull(KotlinUi.of("import a.B\n"))
    }

    @Test
    fun namesAreMatchedAsWholeWords() {
        val ui = KotlinUi.of("BasicText(\"\${lives.count}\")\nval x = myStateManager")!!
        assertTrue(ui.names("lives"))
        assertFalse(ui.names("live"))
        assertFalse(ui.names("stateManager"))
        assertFalse(ui.names("count\$"))
    }
}

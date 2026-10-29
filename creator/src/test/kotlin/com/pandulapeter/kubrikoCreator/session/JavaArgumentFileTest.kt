/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

import kotlin.test.Test
import kotlin.test.assertEquals

class JavaArgumentFileTest {

    @Test
    fun argumentsAreSeparatedByWhitespaceAndGroupedByQuotes() {
        assertEquals(
            listOf("-Xmx1g", "-Dname=a b", "it's", "", "game.MainKt"),
            JavaArgumentFile.parse("-Xmx1g\n  \"-Dname=a b\"\t\"it's\" ''\r\ngame.MainKt\n"),
        )
    }

    @Test
    fun backslashesEscapeOnlyInsideQuotes() {
        assertEquals(listOf("C:\\game\\classes", "a\"b\\c\td"), JavaArgumentFile.parse("C:\\game\\classes \"a\\\"b\\\\c\\td\""))
    }

    @Test
    fun aBackslashAtTheEndOfALineContinuesTheQuotedArgumentWithoutTheNextLinesIndentation() {
        assertEquals(listOf("-cp", "/a:/b:/c", "Main"), JavaArgumentFile.parse("-cp \"/a:\\\n    /b:\\\r\n\t/c\"\nMain"))
    }

    @Test
    fun linesStartingWithAHashAreLeftOut() {
        assertEquals(listOf("-a", "b#c", "-d"), JavaArgumentFile.parse("# a comment\n-a b#c\n  # another one\n-d"))
    }
}

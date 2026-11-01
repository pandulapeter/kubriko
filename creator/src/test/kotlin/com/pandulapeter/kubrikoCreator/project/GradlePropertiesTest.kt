/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GradlePropertiesTest {

    @Test
    fun valuesAreReadAsGradleReadsThem() {
        val text = "# game.name=Commented\nkotlin.code.style=official\ngame.name = My \\u00E9 Game\ngame.vendor:Studio\ngame.description=one \\\n    two\ngame.empty=\n"
        assertEquals("My é Game", GradleProperties.valueOf(text, "game.name"))
        assertEquals("Studio", GradleProperties.valueOf(text, "game.vendor"))
        assertEquals("one two", GradleProperties.valueOf(text, "game.description"))
        assertEquals("", GradleProperties.valueOf(text, "game.empty"))
        assertNull(GradleProperties.valueOf(text, "game.missing"))
        assertNull(GradleProperties.valueOf(text, "game"))
    }

    @Test
    fun aValueReplacesItsLineAndItsContinuationsLeavingTheRestAlone() {
        val text = "a=1\ngame.description=one \\\n    two\n# kept\nb=2\n"
        assertEquals("a=1\ngame.description=new\n# kept\nb=2\n", GradleProperties.withValue(text, "game.description", "new"))
        assertEquals("a=9\ngame.description=one \\\n    two\n# kept\nb=2\n", GradleProperties.withValue(text, "a", "9"))
    }

    @Test
    fun aMissingKeyIsAddedAtTheEnd() {
        assertEquals("a=1\ngame.name=Game\n", GradleProperties.withValue("a=1\n\n\n", "game.name", "Game"))
        assertEquals("game.name=Game\n", GradleProperties.withValue("", "game.name", "Game"))
    }

    @Test
    fun valuesAreEscapedSoThatTheyReadBackTheSame() {
        val value = " Péter's \\ game: #1 = 日本"
        val text = GradleProperties.withValue("", "game.name", value)
        assertEquals("game.name=\\ P\\u00E9ter's \\\\ game: #1 = \\u65E5\\u672C\n", text)
        assertEquals(value, GradleProperties.valueOf(text, "game.name"))
    }
}

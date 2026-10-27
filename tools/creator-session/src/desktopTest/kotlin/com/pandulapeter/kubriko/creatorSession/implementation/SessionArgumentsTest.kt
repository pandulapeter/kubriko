/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SessionArgumentsTest {

    @Test
    fun editModeTakesASceneFolderAndAnOptionalSceneFile() {
        assertEquals(SessionArguments.Edit("scenes", null), SessionArguments.parse(listOf("edit", "scenes")))
        assertEquals(SessionArguments.Edit("scenes", "level.json"), SessionArguments.parse(listOf("edit", "scenes", "level.json")))
    }

    @Test
    fun playModeTakesAMainClass() {
        assertEquals(SessionArguments.Play("game.MainKt"), SessionArguments.parse(listOf("play", "game.MainKt")))
    }

    @Test
    fun missingOrUnknownArgumentsAreRejected() {
        assertFailsWith<IllegalArgumentException> { SessionArguments.parse(emptyList()) }
        assertFailsWith<IllegalArgumentException> { SessionArguments.parse(listOf("edit")) }
        assertFailsWith<IllegalArgumentException> { SessionArguments.parse(listOf("play")) }
        assertFailsWith<IllegalArgumentException> { SessionArguments.parse(listOf("debug", "x")) }
    }
}

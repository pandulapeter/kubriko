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

class BuildScriptTest {

    @Test
    fun bracketsInStringsAndCommentsDontEndABlock() {
        val lines = """
            |commonMain.dependencies {
            |    implementation("a:b:1") // see :)
            |    // UI (Compose {
            |    implementation("c:d:\"}")
            |}
            |after()
        """.trimMargin().lines()
        assertEquals(4, BuildScript.blockEnd(lines, 0))
        assertEquals(4, BuildScript.blockEnd(lines, 0, countsParentheses = true))
    }

    @Test
    fun aBlockOfParenthesesEndsWhereItsLastOneCloses() {
        val lines = "listOf(\n    a(),\n).forEach { it ->\n    it.b {\n    }\n}\nnext".lines()
        assertEquals(0, BuildScript.blockEnd(lines, 0))
        assertEquals(5, BuildScript.blockEnd(lines, 0, countsParentheses = true))
    }
}

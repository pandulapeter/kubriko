/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameSizeUpgradeTest {

    @Test
    fun aGameHandedFillMaxSizeIsCalledWithoutItAndItsImportsGo() {
        val before = """
            |package com.example.mygame
            |
            |import androidx.compose.foundation.layout.fillMaxSize
            |import androidx.compose.ui.Modifier
            |import androidx.compose.ui.window.ComposeViewport
            |
            |fun main() {
            |    ComposeViewport {
            |        Game(
            |            modifier = Modifier.fillMaxSize(),
            |        )
            |    }
            |}
            |
        """.trimMargin()
        assertFalse(GameSizeUpgrade.isDone(before))
        val after = GameSizeUpgrade.change(before)
        assertEquals("package com.example.mygame\n\nimport androidx.compose.ui.window.ComposeViewport\n\nfun main() {\n    ComposeViewport {\n        Game()\n    }\n}\n", after)
        assertTrue(GameSizeUpgrade.isDone(after))
    }

    @Test
    fun anImportStillInUseStays() {
        val before = "import androidx.compose.foundation.layout.fillMaxSize\nimport androidx.compose.ui.Modifier\n\nval other = Modifier\n\nfun x() {\n    Game(\n        modifier = Modifier.fillMaxSize(),\n    )\n}\n"
        assertEquals("import androidx.compose.ui.Modifier\n\nval other = Modifier\n\nfun x() {\n    Game()\n}\n", GameSizeUpgrade.change(before))
    }

    @Test
    fun aGameHandedAnythingElseIsTheUsersOwnAndCountsAsDone() = listOf(
        "Game(\n    modifier = Modifier.fillMaxSize().background(Color.Black),\n)",
        "Game(modifier = Modifier.fillMaxSize())",
        "Game()",
    ).forEach { call ->
        assertTrue(GameSizeUpgrade.isDone(call), call)
        assertEquals(call, GameSizeUpgrade.change(call))
    }
}

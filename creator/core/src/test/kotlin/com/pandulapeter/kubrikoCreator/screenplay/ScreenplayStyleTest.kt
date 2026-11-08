/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStyle.Problem
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStyle.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenplayStyleTest {
    private fun rulesOf(text: String, numbers: Set<String> = emptySet(), isWholePage: Boolean = true) =
        ScreenplayStyle.problemsOf(text, "screenplay/README.md", mapOf(".kubriko/actors/Ball.json" to numbers), isWholePage).map { it.rule to it.excerpt }

    @Test
    fun `a page in the shape the rules ask for keeps them`() {
        val page = """
            # The hero

            A knight who jumps between floating islands, with a fixed aspect ratio and a known number of lives.

            ## Moves

            The [hero](../.kubriko/actors/Player.json) runs at the speed its [speed](../.kubriko/actors/Player.json) gives, and knows
            every island by now's standards? No: by heart. We see it from the side.

            ## Planned

            - Wall jump
            - A shield that blocks one hit
              which lasts a moment
            ### Later
            - Bosses
        """.trimIndent().replace("now's standards? No: by heart.", "heart.")
        assertEquals(emptyList(), rulesOf(page, numbers = setOf("240")))
    }

    @Test
    fun `history and dates are named, but not the same words meaning something else`() {
        assertEquals(
            listOf(Rule.HISTORY to "used to", Rule.HISTORY to "now", Rule.HISTORY to "was changed", Rule.HISTORY to "no longer", Rule.HISTORY to "2026-11-08", Rule.HISTORY to "fixed the bug"),
            rulesOf("It used to fly.\nThe hero now jumps twice.\nThe speed was changed.\nIt no longer flies.\nSince 2026-11-08.\nWe fixed the bug in it.").filter { it.first == Rule.HISTORY },
        )
        assertEquals(emptyList(), rulesOf("A fixed camera. The known world. Snow falls. `now` in code. [A page](now.md)"))
    }

    @Test
    fun `narrating the conversation or the work is named`() {
        assertEquals(
            listOf(Rule.NARRATION to "I added", Rule.NARRATION to "we decided", Rule.NARRATION to "as requested", Rule.NARRATION to "I've changed"),
            rulesOf("I added a jump.\nThen we decided on two.\nShorter, as requested.\nI've changed it.").filter { it.first == Rule.NARRATION },
        )
        assertEquals(emptyList(), rulesOf("We see the hero. I is a letter. Players decide where to go."))
    }

    @Test
    fun `code is named once per block, and nothing inside it is checked`() {
        assertEquals(listOf(Rule.CODE to "```kotlin"), rulesOf("Intro.\n\n```kotlin\nval used to = now\n```\n\nAfter."))
    }

    @Test
    fun `a Property's number copied into prose is named, but not inside a link or another number`() {
        assertEquals(
            listOf(Rule.COPIED_NUMBER to "240"),
            rulesOf(
                "The [ball](../.kubriko/actors/Ball.json) moves at 240 pixels a second.\n" +
                    "The [ball](../.kubriko/actors/Ball.json): not 2400, 1240, 240.5, or [240](a.md), or `240`.\n" +
                    "The game has 240 levels, on a line that doesn't mention the ball.",
                numbers = setOf("240"),
            ),
        )
    }

    @Test
    fun `Planned holds a list of short items`() {
        val page = "# Game\n\n## Planned\n\nSome prose here.\n\n- " + "word ".repeat(40) + "\n- Short\n"
        assertEquals(listOf(Rule.PROSE_IN_PLANNED, Rule.LONG_PLANNED_ITEM), rulesOf(page).map { it.first })
    }

    @Test
    fun `a whole page past the limit is named, but not a section of one`() {
        val long = "# Game\n\n" + "A sentence about the game. ".repeat(300)
        assertEquals(listOf(Rule.TOO_LONG), rulesOf(long).map { it.first })
        assertEquals(emptyList(), rulesOf(long, isWholePage = false))
    }

    @Test
    fun `a problem says where it is and what to change`() {
        assertEquals(
            "line 2, \"used to\": Describe the game as it is, in the present tense, without its history.",
            Problem(Rule.HISTORY, 2, "used to").toString(),
        )
    }
}

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PageIndexTextTest {

    @Test
    fun `each page is listed with its title and summary, the first page first`() {
        val index = PageIndexText.of(
            mapOf(
                "levels/cave" to "# The cave\n\nDark, and full of bats.\n",
                "README" to "# The game\n\nA platformer about a knight\nwho jumps.\n\n## Planned\n\n- More",
                "notes" to "Just a line.",
                "empty" to "",
            ),
        )
        assertEquals(
            """
            The screenplay (the game's design in its own words, in screenplay/; read_pages reads pages):
            - README: The game — A platformer about a knight who jumps.
            - empty
            - levels/cave: The cave — Dark, and full of bats.
            - notes — Just a line.
            """.trimIndent(),
            index,
        )
        assertNull(PageIndexText.of(emptyMap()))
    }

    @Test
    fun `a long summary is cut short, and a screenplay too large lists its folders instead`() {
        val long = PageIndexText.of(mapOf("README" to "# Game\n\n" + "word ".repeat(100)))!!
        assertTrue(long.endsWith("…"))
        val pages = (1..300).associate { "levels/level-$it" to "# Level $it\n\n" + "A level with a long description of what happens in it. ".repeat(3) } +
            ("README" to "# The game\n\nA platformer.") + ("items/sword" to "# Sword")
        val folded = PageIndexText.of(pages)!!
        assertEquals(
            """
            The screenplay (the game's design in its own words, in screenplay/; read_pages reads pages):
            - README: The game — A platformer.
            - items/ (1 page)
            - levels/ (300 pages)
            """.trimIndent(),
            folded,
        )
    }

    @OptIn(EditorMutation::class)
    @Test
    fun `a conversation starts with the index after the digest`() = runTest {
        val context = commandContext()
        context.screenplay.write("README", "# The game\n\nA platformer.")
        val start = ConversationStart.of(context)
        assertTrue("\n\nThe screenplay (" in start && start.trimEnd().endsWith("- README: The game — A platformer."), start)
    }
}

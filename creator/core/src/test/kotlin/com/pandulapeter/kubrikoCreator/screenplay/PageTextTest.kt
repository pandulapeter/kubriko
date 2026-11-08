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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PageTextTest {
    private val hero = """
        # The hero

        A knight who jumps
        between floating islands.

        ## Moves

        Runs and jumps.

        ### Double jump

        Only after the boots.

        ```
        ## Not a heading
        ```

        ## Planned

        - Wall jump
    """.trimIndent()

    @Test
    fun theTitleAndTheSummaryAreRead() {
        assertEquals("The hero", PageText.titleOf(hero))
        assertEquals("A knight who jumps between floating islands.", PageText.summaryOf(hero))
        assertEquals("Only a paragraph.", PageText.summaryOf("Only a paragraph.\n\n## Then a section"))
        assertNull(PageText.titleOf("No heading at all."))
        assertNull(PageText.summaryOf("# Title\n\n## Straight to a section"))
        assertNull(PageText.summaryOf(""))
    }

    @Test
    fun sectionsReachTheNextHeadingOfTheirLevelAndSkipCode() {
        val sections = PageText.sectionsOf(hero)
        assertEquals(listOf("Moves" to 2, "Double jump" to 3, "Planned" to 2), sections.map { it.heading to it.level })
        assertEquals(5..16, sections[0].lines)
    }

    @Test
    fun aSectionIsReplacedWithItsSubsectionsAndTheRestKept() {
        val replaced = PageText.withSectionReplaced(hero, "moves", "## Moves\n\nRuns, jumps, and rolls.")
        assertEquals(
            """
            # The hero

            A knight who jumps
            between floating islands.

            ## Moves

            Runs, jumps, and rolls.

            ## Planned

            - Wall jump
            """.trimIndent(),
            replaced,
        )
        assertNull(PageText.withSectionReplaced(hero, "Enemies", "## Enemies"))
    }

    @Test
    fun windowsLineBreaksAreKeptWhenASectionIsReplaced() {
        val text = "# A\r\n\r\n## B\r\nOld\r\n\r\n## C\r\nKept\r\n"
        assertEquals("# A\r\n\r\n## B\r\nNew\r\n\r\n## C\r\nKept\r\n", PageText.withSectionReplaced(text, "B", "## B\nNew\n"))
    }

    @Test
    fun replacingTheLastSectionKeepsTheFinalLineBreak() =
        assertEquals("# T\n\n## Planned\n\n- b\n", PageText.withSectionReplaced("# T\n\n## Planned\n\n- a\n", "Planned", "## Planned\n\n- b"))

    @Test
    fun thePlannedSectionIsFoundByItsHeadingAtLevelTwoOnly() {
        assertEquals("Planned", PageText.plannedSectionOf(hero)?.heading)
        assertNull(PageText.plannedSectionOf("# Game\n\n### Planned\n\n- Not this one"))
    }
}

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import com.pandulapeter.kubrikoCreator.foundation.NameMatch.Quality
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NameMatchingTest {

    @Test
    fun aBlankQueryMatchesEveryNameHighlightingNothing() = assertEquals(NameMatch(Quality.EXACT, emptyList()), NameMatching.match("Ball", "  "))

    @Test
    fun theWholeNameIgnoringCaseAndSurroundingSpacesIsExact() = assertEquals(NameMatch(Quality.EXACT, listOf(0..3)), NameMatching.match("Ball", " bALL "))

    @Test
    fun theStartOfTheNameIsAPrefix() = assertEquals(NameMatch(Quality.PREFIX, listOf(0..1)), NameMatching.match("Ball", "ba"))

    @Test
    fun theStartOfAWordComesBeforeAnywhereElse() {
        assertEquals(NameMatch(Quality.WORD_START, listOf(3..6)), NameMatching.match("BigBall", "ball"))
        assertEquals(NameMatch(Quality.WORD_START, listOf(4..7)), NameMatching.match("big_ball", "ball"))
        assertEquals(NameMatch(Quality.WORD_START, listOf(5..5)), NameMatching.match("level2", "2"))
        assertEquals(NameMatch(Quality.WORD_START, listOf(9..12)), NameMatching.match("Football Ball", "ball"), "the word's start is found past an earlier match inside a word")
        assertEquals(NameMatch(Quality.CONTAINS, listOf(4..7)), NameMatching.match("Football", "ball"))
    }

    @Test
    fun theQuerysSpacesAreIgnored() = assertEquals(NameMatch(Quality.PREFIX, listOf(0..6)), NameMatching.match("BigBallSpawner", "big ball"))

    @Test
    fun lettersInOrderMatchWhenTheFirstStartsAWord() {
        assertEquals(NameMatch(Quality.SCATTERED, listOf(0..1, 4..4)), NameMatching.match("Brick", "brk"))
        assertEquals(NameMatch(Quality.SCATTERED, listOf(0..0, 3..3)), NameMatching.match("BigBall", "bb"))
        assertEquals(NameMatch(Quality.SCATTERED, listOf(3..3, 6..6)), NameMatching.match("theBigBall", "bb"))
        assertNull(NameMatching.match("Ball", "lb"), "out of order")
        assertNull(NameMatching.match("Tablet", "bt"), "its first letter starts no word")
        assertNull(NameMatching.match("Brick", "brkk"))
    }

    @Test
    fun matchesSaysWhetherThereIsAMatch() {
        assertTrue(NameMatching.matches("Brick", "brk"))
        assertFalse(NameMatching.matches("Brick", "x"))
    }
}

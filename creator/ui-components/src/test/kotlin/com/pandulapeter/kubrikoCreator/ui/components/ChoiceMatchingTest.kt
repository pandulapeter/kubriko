/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ChoiceMatchingTest {

    @Test
    fun anEmptyQueryKeepsEveryOptionInOrder() = assertEquals(
        listOf("Football", "Ball", "Ballistic"),
        matchingOptions("  ", listOf("Football", "Ball", "Ballistic"), titleOf = { it }),
    )

    @Test
    fun exactMatchesComeBeforeThoseStartingWithTheQueryBeforeThoseContainingIt() = assertEquals(
        listOf("Ball", "Ballistic", "Football"),
        matchingOptions("ball", listOf("Ballistic", "Ball", "Football"), titleOf = { it }),
    )

    @Test
    fun spacesInTheQueryAreIgnored() = assertEquals(
        listOf("BigBall"),
        matchingOptions("big ball", listOf("BigBall", "Bat"), titleOf = { it }),
    )

    @Test
    fun lettersInOrderMatchAfterEveryCloserMatch() = assertEquals(
        listOf("Brk", "BrokenWall", "Brick"),
        matchingOptions("brk", listOf("BrokenWall", "Ball", "Brick", "Brk"), titleOf = { it }),
    )

    @Test
    fun aSecondNameMatchesToo() = assertEquals(
        listOf(2),
        matchingOptions("pause", listOf(1, 2), titleOf = { "Key $it" }, alsoMatching = { if (it == 2) "MediaPlayPause" else null }),
    )

    @Test
    fun nothingMatchingGivesNoOption() = assertEquals(
        emptyList(),
        matchingOptions("zzz", listOf("Ball", "Bat"), titleOf = { it }),
    )
}

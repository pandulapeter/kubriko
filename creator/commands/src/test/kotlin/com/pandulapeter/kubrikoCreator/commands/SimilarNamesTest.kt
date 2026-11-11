/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import kotlin.test.Test
import kotlin.test.assertEquals

class SimilarNamesTest {

    @Test
    fun aNameOfAnotherCaseOrWithATypoIsOffered() {
        assertEquals(listOf("Player"), SimilarNames.of("player", listOf("Player", "Wall")))
        assertEquals(listOf("Player"), SimilarNames.of("Plyer", listOf("Player", "Wall")))
        assertEquals(listOf("Paddle"), SimilarNames.of("Paddel", listOf("Paddle", "Ball")))
        assertEquals(listOf("game-over"), SimilarNames.of("game_over", listOf("game-over", "title")))
    }

    @Test
    fun aShortNameIsMatchedOnlyIgnoringCaseOrByALetterMoreOrFewer() {
        assertEquals(emptyList(), SimilarNames.of("Wall", listOf("Ball", "Hall")))
        assertEquals(listOf("WALL"), SimilarNames.of("Wall", listOf("Ball", "WALL")))
        assertEquals(emptyList(), SimilarNames.of("i2", listOf("i0", "i1")))
        assertEquals(listOf("speed"), SimilarNames.of("sped", listOf("speed", "seed")))
    }

    @Test
    fun aSharedFolderDoesNotMakeNamesLookAlike() {
        assertEquals(emptyList(), SimilarNames.of("drawable/ghost.png", listOf("drawable/ball.png")))
        assertEquals(emptyList(), SimilarNames.of("levels/1-1", listOf("levels/1-2")))
    }

    @Test
    fun aNameHoldingTheWantedOneIsOfferedAfterTypos() {
        assertEquals(listOf("levels/1-1"), SimilarNames.of("1-1", listOf("levels/1-1", "levels/1-2")))
        assertEquals(listOf("drawable/ball.png"), SimilarNames.of("drawable/ball", listOf("drawable/ball.png", "drawable/wall.png")))
        assertEquals(listOf("Spacebar"), SimilarNames.of("Space", listOf("Spacebar", "Enter")))
        assertEquals(listOf("Players", "PlayerShip"), SimilarNames.of("Player", listOf("PlayerShip", "Players", "Enemy")))
    }

    @Test
    fun atMostThreeAreOfferedTheClosestFirst() {
        assertEquals(listOf("AltLeft", "CtrlLeft", "ShiftLeft"), SimilarNames.of("Left", listOf("DirectionLeft", "ShiftLeft", "CtrlLeft", "AltLeft", "Right")))
    }

    @Test
    fun nothingIsOfferedForABlankNameOrTheNameItself() {
        assertEquals(emptyList(), SimilarNames.of(" ", listOf("Player")))
        assertEquals(emptyList(), SimilarNames.of("Player", listOf("Player")))
    }

    @Test
    fun theRejectionNamesTheSimilarNamesOnlyWhenThereAreAny() {
        assertEquals(
            Rejection(CommandProblem.TIMER_UNKNOWN_SIMILAR, "Ball", "spwan", "spawn"),
            SimilarNames.rejection(CommandProblem.TIMER_UNKNOWN, CommandProblem.TIMER_UNKNOWN_SIMILAR, "spwan", listOf("spawn"), "Ball", "spwan"),
        )
        assertEquals(
            Rejection(CommandProblem.TIMER_UNKNOWN, "Ball", "blink"),
            SimilarNames.rejection(CommandProblem.TIMER_UNKNOWN, CommandProblem.TIMER_UNKNOWN_SIMILAR, "blink", listOf("spawn"), "Ball", "blink"),
        )
    }
}

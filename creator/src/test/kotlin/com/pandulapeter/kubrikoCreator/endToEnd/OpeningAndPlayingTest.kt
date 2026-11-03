/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.endToEnd

import com.pandulapeter.kubrikoCreator.context.BreakoutFixture
import com.pandulapeter.kubrikoCreator.endToEnd.ImageChecks.boundsOf
import com.pandulapeter.kubrikoCreator.endToEnd.ImageChecks.countOf
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * The harness's own scenario: Breakout opens in Kubriko Creator, builds, and plays, drawing its Actors where its Scene
 * places them.
 */
class OpeningAndPlayingTest {

    @Test
    fun breakoutOpensBuildsAndPlaysWithItsActorsWhereTheSceneHasThem() = EndToEnd.scenario("opening-and-playing") {
        step("Kubriko Creator's window shows") {
            val screen = captureScreen("creator")
            val drawn = screen.width * screen.height - screen.countOf(BLACK, tolerance = 0)
            assertTrue(drawn >= WINDOW_AREA * 0.95, "Only $drawn pixels of the screen were drawn, less than Kubriko Creator's window.")
        }
        step("start_game builds and starts the game") {
            val started = tools.call("start_game", timeout = 10.minutes)
            assertTrue(!started.isError && started["hasStarted"]?.jsonPrimitive?.boolean == true, started.text)
        }
        val first = step("The game draws the Gold Bricks in their column") {
            captureGame("game").also { game ->
                val gold = assertNotNull(game.boundsOf(GOLD), "No Gold Brick was drawn.")
                // The level puts its Gold Bricks in the fifth column, whose middle is 30 units left of the screen's.
                assertTrue(abs(gold.centerX - (game.width / 2 - 30)) <= 2, "The Gold Bricks are at $gold, not in their column.")
                assertTrue(abs(gold.width - BreakoutFixture.brick.width.toInt()) <= 2, "The Gold Bricks are ${gold.width} pixels wide.")
                assertTrue(game.countOf(BRICK) > game.countOf(GOLD) * 5, "The other Bricks weren't drawn around them.")
                assertTrue(game.countOf(BALL) > 0, "The Ball wasn't drawn.")
            }
        }
        step("The Game's two texts at the top left are one under the other") {
            val text = assertNotNull(first.boundsOf(TEXT), "The Game's texts weren't drawn.")
            val lineHeight = BreakoutFixture.game.texts.first().size
            assertTrue(text.height > lineHeight * 1.5f, "The texts are only ${text.height} pixels tall, so they are drawn over each other.")
        }
        step("The Ball moves while the rest stays") {
            val second = captureGame("game-later")
            val difference = ImageChecks.differenceBetween(first, second)
            assertTrue(difference > 0f && difference < 0.05f, "${difference * 100}% of the game changed between two screenshots.")
        }
    }

    private companion object {
        const val BLACK = 0x000000
        const val GOLD = 0xFFC857
        const val BRICK = 0x6060AA
        const val BALL = 0xFFFFFF
        const val TEXT = 0x202020
        const val WINDOW_AREA = 1280 * 820
    }
}

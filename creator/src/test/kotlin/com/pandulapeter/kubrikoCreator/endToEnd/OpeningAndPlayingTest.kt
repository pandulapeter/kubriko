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
import java.awt.image.BufferedImage
import java.io.File
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
            fun drawnPixels(screen: BufferedImage) = screen.width * screen.height - screen.countOf(BLACK, tolerance = 0)
            val screen = awaitScreen("creator") { drawnPixels(it) >= MINIMUM_WINDOW_AREA }
            assertTrue(drawnPixels(screen) >= MINIMUM_WINDOW_AREA, "Only ${drawnPixels(screen)} pixels of the screen were drawn, less than Kubriko Creator's smallest window.")
        }
        step("Kubriko Creator keeps its settings in the scenario's folder") {
            assertTrue(File(outputDirectory, "settings/recent-projects.json").readText().contains(project.name))
        }
        step("start_game builds and starts the game") {
            val started = tools.call("start_game", timeout = 10.minutes)
            assertTrue(!started.isError && started["hasStarted"]?.jsonPrimitive?.boolean == true, started.text)
        }
        val first = step("take_screenshot shows the game") { captureGame("game") }
        // take_screenshot scales the game down to at most 1280 pixels wide, so how many pixels a unit is depends on the display.
        val scale = step("The game draws the Gold Bricks in their column") {
            val gold = assertNotNull(first.boundsOf(GOLD), "No Gold Brick was drawn.")
            // The column is cut off at the top of a short window, but always drawn as wide as a Brick.
            val scale = gold.width / BreakoutFixture.brick.width
            assertTrue(scale in 0.25f..4f, "The Gold Bricks are ${gold.width} pixels wide, which no display scale explains.")
            // The level puts its Gold Bricks in the fifth column, whose middle is 30 units left of the screen's.
            assertTrue(abs(gold.centerX - (first.width / 2 - 30 * scale)) <= 2, "The Gold Bricks are at $gold, not in their column (at $scale pixels a unit).")
            assertTrue(first.countOf(BRICK) > first.countOf(GOLD) * 5, "The other Bricks weren't drawn around them.")
            val ball = assertNotNull(first.boundsOf(BALL), "The Ball wasn't drawn.")
            val ballSize = BreakoutFixture.ball.width * scale
            assertTrue(abs(ball.width - ballSize) <= 2 && abs(ball.height - ballSize) <= 2, "The Ball is drawn ${ball.width}×${ball.height} pixels large (at $scale pixels a unit).")
            scale
        }
        step("The Game's two texts at the top left are one under the other") {
            val text = assertNotNull(first.boundsOf(TEXT), "The Game's texts weren't drawn.")
            val lineHeight = BreakoutFixture.game.texts.first().size * scale
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
        /**
         * The smallest window Kubriko Creator lets itself be (KubrikoCreatorApp.kt), at the display's scale of 1.
         */
        const val MINIMUM_WINDOW_AREA = 960 * 600
    }
}

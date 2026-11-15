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

import com.pandulapeter.kubrikoCreator.endToEnd.ImageChecks.boundsOf
import com.pandulapeter.kubrikoCreator.endToEnd.ImageChecks.countOf
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Rectangle
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * Blocks that only a played game can show working, each given to the Breakout fixture through the tools and judged by
 * pixels: the Paddle steered with Move with keys, an Actor made half as strong by Set the opacity, one moving down at 90
 * degrees, one fading out with Animate and removed at its end, and one crossing the Scene and removed once it leaves its
 * bounds; and a Manager hearing Bricks removed and counting them to go to a win Scene once none is left, while restarting
 * their level removes none. The project stays at template version 7 throughout, so this is how an older project plays them.
 */
class PlayingBlocksTest {

    @Test
    fun blocksMoveFadeAndRemoveActorsAndAManagerCountsThemInAPlayedGame() = EndToEnd.scenario("playing-blocks") {
        step("The Paddle moves with keys, new Actor types use the Blocks, a Manager watches them, and three Scenes place them") {
            replacePaddleEvents()
            labTypes.forEach { succeed("create_actor_type", it) }
            succeed("add_event", mapOf("actorType" to "Brick", "type" to "keyPressed", "key" to "B", "blocks" to listOf(mapOf("type" to "removeSelf"))))
            listOf(LAB_SCENE, SHORT_SCENE, WIN_SCENE).forEach { succeed("create_scene", mapOf("scene" to it)) }
            succeed("create_manager_type", referee)
            succeed("set_scene_settings", mapOf("scene" to LAB_SCENE, "x" to 0, "y" to 0, "width" to LAB_WIDTH, "height" to LAB_HEIGHT))
            succeed("place_actors", mapOf("scene" to LAB_SCENE, "actors" to labPlacements.map { (type, x, y) -> mapOf("actorType" to type, "x" to x, "y" to y) }))
            succeed("place_actors", mapOf("scene" to SHORT_SCENE, "actors" to List(BRICKS) { index -> mapOf("actorType" to "Brick", "x" to (index - 1) * 100, "y" to -100) }))
            succeed("place_actors", mapOf("scene" to WIN_SCENE, "actors" to listOf(mapOf("actorType" to "Trophy", "x" to 0, "y" to 0))))
        }
        start(LAB_SCENE)
        val first = step("The lab Scene shows its Actors") {
            awaitGame("lab") { it.boundsOf(SOLID) != null && it.boundsOf(PADDLE) != null && it.boundsOf(DIVER) != null && it.boundsOf(GHOST) != null }
        }
        val scale = step("The Solid square is drawn as large as its type") {
            val solid = assertNotNull(first.boundsOf(SOLID), "The Solid square wasn't drawn.")
            assertTrue(abs(solid.width - solid.height) <= 2, "The Solid square is drawn ${solid.width}×${solid.height} pixels large.")
            solid.width / SQUARE_SIZE
        }
        val background = step("The background is one color around the squares") { first.backgroundBelow(assertNotNull(first.boundsOf(SOLID)), scale) }
        step("Set the opacity to 50 draws the Faded square half as strong as the Solid one") { checkFaded(first, background, scale) }
        step("Move in a direction at 90 degrees moves the Diver down") {
            val diver = assertNotNull(first.boundsOf(DIVER))
            val later = awaitGame("diver-later") { image -> image.boundsOf(DIVER)?.let { it.y - diver.y >= DIVER_DISTANCE * scale } == true }
            val moved = assertNotNull(later.boundsOf(DIVER), "The Diver is gone.")
            assertTrue(moved.y - diver.y >= DIVER_DISTANCE * scale, "The Diver only moved from $diver to $moved.")
            assertTrue(abs(moved.centerX - diver.centerX) <= 2, "The Diver moved sideways, from $diver to $moved.")
        }
        step("Move with keys moves the Paddle right while the right arrow is held, and stops it once released") { checkPaddle(scale) }
        step("Animate fades the Ghost out once F is pressed, and removes it at the end") { checkGhost(background) }
        step("The Leaver crosses the Scene once L is pressed, and is removed once it leaves it") { checkLeaver() }
        step("The lab stops") { succeed("stop_game") }
        start(SHORT_SCENE)
        step("Restarting the short level removes none of its Bricks as far as the Manager hears") {
            awaitGame("short") { it.countOf(BRICK) > 0 }
            val restarted = pressKeys("short-restarted", listOf("R") to KEY_TAP, emptyList<String>() to SETTLING, listOf("R") to KEY_TAP, emptyList<String>() to SETTLING)
            assertTrue(restarted.countOf(BRICK) > 0, "The Bricks are gone after restarting.")
            assertTrue(restarted.boundsOf(TROPHY) == null, "The win Scene shows after restarting.")
            val console = succeed("read_console").text
            val added = BRICK_ADDED.toRegex(RegexOption.LITERAL).findAll(console).count()
            assertTrue(added == BRICKS * 3, "The Manager heard $added Bricks added, not ${BRICKS * 3}, so the level didn't start and restart twice.")
            assertFalse(BRICK_GONE in console, "Restarting the level counted its Bricks as removed.")
        }
        step("Removing every Brick goes to the win Scene") {
            pressKeys("short-cleared", listOf("B") to KEY_TAP)
            val won = awaitGame("win") { it.boundsOf(TROPHY) != null }
            assertNotNull(won.boundsOf(TROPHY), "The win Scene never showed.")
            assertTrue(won.countOf(BRICK) == 0, "Bricks are still drawn in the win Scene.")
            assertTrue(BRICK_GONE in succeed("read_console").text, "The Manager never heard a Brick removed.")
            succeed("stop_game")
        }
    }

    private fun EndToEndScenario.start(scene: String) = step("start_game builds the game and starts it in the $scene Scene") {
        val started = tools.call("start_game", mapOf("scene" to scene), timeout = 10.minutes)
        assertTrue(!started.isError && started["hasStarted"]?.jsonPrimitive?.boolean == true, started.text)
    }

    /**
     * Replaces the Paddle's four Events, which steer it with velocities and keys of their own, with one moving it with keys.
     */
    private fun EndToEndScenario.replacePaddleEvents() {
        repeat(4) { succeed("remove_event", mapOf("actorType" to "Paddle", "eventIndex" to 0)) }
        val moveWithKeys = mapOf("type" to "moveWithKeys", "keys" to "arrows", "movesDiagonally" to false, "speed" to PADDLE_SPEED)
        succeed("add_event", mapOf("actorType" to "Paddle", "type" to "everyFrame", "blocks" to listOf(moveWithKeys)))
    }

    /**
     * The color the game shows a little below [square], where nothing is drawn, after checking that it is one color there.
     */
    private fun BufferedImage.backgroundBelow(square: Rectangle, scale: Float): Int {
        val y = (square.maxY + BACKGROUND_GAP * scale).roundToInt()
        val color = getRGB(square.centerX.roundToInt(), y) and 0xFFFFFF
        val strip = Rectangle(square.x, y - 2, square.width, 5)
        assertTrue(countOf(color, strip) == strip.width * strip.height, "The game isn't one color below the Solid square, at $strip.")
        return color
    }

    private fun checkFaded(image: BufferedImage, background: Int, scale: Float) {
        val solid = assertNotNull(image.boundsOf(SOLID))
        val center = image.getRGB((solid.centerX + (FADED_X - SOLID_X) * scale).roundToInt(), solid.centerY.roundToInt())
        val expected = halfway(SOLID, background)
        assertTrue(
            ImageChecks.matches(center, expected, tolerance = BLENDING_TOLERANCE),
            "The Faded square is #%06X, not halfway between the Solid square's #%06X and the background's #%06X.".format(center and 0xFFFFFF, SOLID, background),
        )
        val faded = assertNotNull(image.boundsOf(expected, tolerance = BLENDING_TOLERANCE), "The Faded square wasn't drawn.")
        assertTrue(abs(faded.width - solid.width) <= 2 && abs(faded.height - solid.height) <= 2, "The Faded square is $faded, the Solid one $solid.")
    }

    private fun EndToEndScenario.checkPaddle(scale: Float) {
        val before = assertNotNull(captureGame("paddle-before").boundsOf(PADDLE), "The Paddle wasn't drawn.")
        val held = assertNotNull(pressKeys("paddle-moved", listOf("RIGHT") to PADDLE_HOLD).boundsOf(PADDLE), "The Paddle is gone.")
        val moved = held.centerX - before.centerX
        assertTrue(moved >= PADDLE_MINIMUM_DISTANCE * scale, "The Paddle moved only $moved pixels right, from $before to $held.")
        assertTrue(abs(held.centerY - before.centerY) <= 1, "The Paddle moved up or down, from $before to $held.")
        succeed("wait", mapOf("milliseconds" to SETTLING))
        val after = assertNotNull(captureGame("paddle-released").boundsOf(PADDLE), "The Paddle is gone.")
        assertTrue(abs(after.centerX - held.centerX) <= 2, "The Paddle kept moving once released, from $held to $after.")
    }

    private fun EndToEndScenario.checkGhost(background: Int) {
        val ghost = assertNotNull(captureGame("ghost-before").boundsOf(GHOST), "The Ghost wasn't drawn.")
        val fading = pressKeys("ghost-fading", listOf("F") to GHOST_HOLD).getRGB(ghost.centerX.roundToInt(), ghost.centerY.roundToInt())
        assertTrue(
            isBetween(fading, GHOST, background),
            "The Ghost is #%06X while fading, not between its #%06X and the background's #%06X.".format(fading and 0xFFFFFF, GHOST, background),
        )
        awaitConsole(GHOST_GONE)
        val gone = captureGame("ghost-gone")
        assertTrue(gone.countOf(GHOST, tolerance = 0) == 0, "The Ghost is still drawn.")
        assertTrue(ImageChecks.matches(gone.getRGB(ghost.centerX.roundToInt(), ghost.centerY.roundToInt()), background), "Something is left where the Ghost was.")
    }

    /**
     * The Leaver starts right of the lab's bounds, so it fires its left the Scene Event only once it came in and crossed.
     * Where the screen is wider than the bounds, it would still show past their left edge had it not been removed.
     */
    private fun EndToEndScenario.checkLeaver() {
        val waiting = captureGame("leaver-waiting").boundsOf(LEAVER)
        succeed("press_keys", mapOf("keys" to listOf("L"), "milliseconds" to KEY_TAP))
        fun isPastTheMiddle(image: BufferedImage) = image.boundsOf(LEAVER)?.let { it.centerX < image.width / 2 } == true
        val crossing = awaitGame("leaver-crossing", isReady = ::isPastTheMiddle)
        assertTrue(isPastTheMiddle(crossing), "The Leaver never crossed the middle of the Scene, from $waiting.")
        assertFalse(LEAVER_GONE in succeed("read_console").text, "The Leaver was removed before it left the Scene.")
        awaitConsole(LEAVER_GONE)
        assertTrue(captureGame("leaver-gone").boundsOf(LEAVER) == null, "The Leaver is still drawn after it was removed.")
    }

    /**
     * Whether every channel of [pixel] lies strictly between those of [first] and [second], away from both.
     */
    private fun isBetween(pixel: Int, first: Int, second: Int) = !ImageChecks.matches(pixel, first) && !ImageChecks.matches(pixel, second) &&
        intArrayOf(16, 8, 0).all { shift ->
            val value = pixel shr shift and 0xFF
            value in minOf(first shr shift and 0xFF, second shr shift and 0xFF)..maxOf(first shr shift and 0xFF, second shr shift and 0xFF)
        }

    private fun halfway(first: Int, second: Int) = intArrayOf(16, 8, 0).fold(0) { color, shift ->
        color or (((first shr shift and 0xFF) + (second shr shift and 0xFF)) / 2 shl shift)
    }

    private companion object {
        const val LAB_SCENE = "lab"
        const val SHORT_SCENE = "short"
        const val WIN_SCENE = "win"
        const val LAB_WIDTH = 1000
        const val LAB_HEIGHT = 700
        const val SQUARE_SIZE = 100f
        const val SOLID_X = -250
        const val FADED_X = -80
        const val SOLID = 0xE04040
        const val GHOST = 0x3060E0
        const val DIVER = 0x40C040
        const val LEAVER = 0xE0A020
        const val TROPHY = 0xA040E0
        const val PADDLE = 0x6060AA
        const val BRICK = 0x6060AA
        const val PADDLE_SPEED = 300
        const val PADDLE_HOLD = 500
        const val PADDLE_MINIMUM_DISTANCE = 60f
        const val DIVER_DISTANCE = 20f
        const val KEY_TAP = 150

        /**
         * About halfway through the Ghost's second of fading, so that its screenshot catches it partly faded.
         */
        const val GHOST_HOLD = 400
        const val SETTLING = 500
        const val BACKGROUND_GAP = 20f
        const val BLENDING_TOLERANCE = 12
        const val GHOST_GONE = "The Ghost is gone."
        const val LEAVER_GONE = "The Leaver is gone."
        const val BRICK_GONE = "A Brick is gone."
        const val BRICK_ADDED = "A Brick is here."
        const val BRICKS = 3

        private fun square(name: String, color: Int, size: Int = SQUARE_SIZE.toInt(), events: List<Map<String, Any?>> = emptyList()) =
            mapOf("name" to name, "shape" to "rectangle", "color" to "#%06X".format(color), "width" to size, "height" to size, "events" to events)

        private fun event(type: String, vararg blocks: Map<String, Any?>, key: String? = null, otherActorType: String? = null) =
            mapOf("type" to type, "key" to key, "otherActorType" to otherActorType, "blocks" to blocks.toList()).filterValues { it != null }

        private fun moveInDirection(direction: Int, speed: Int) = mapOf("type" to "moveInDirection", "direction" to direction, "speed" to speed)

        private fun log(message: String) = mapOf("type" to "log", "message" to message)

        val labTypes = listOf(
            square("Solid", SOLID),
            square("Faded", SOLID, events = listOf(event("addedToScene", mapOf("type" to "setOpacity", "percent" to 50)))),
            square(
                "Ghost",
                GHOST,
                events = listOf(
                    event(
                        "keyPressed",
                        mapOf("type" to "animate", "value" to mapOf("type" to "opacity", "from" to 100, "to" to 0), "seconds" to 1, "easing" to "linear", "removesActor" to true),
                        key = "F",
                    ),
                ),
            ),
            square("Diver", DIVER, size = 60, events = listOf(event("addedToScene", moveInDirection(direction = 90, speed = 30)))),
            square(
                "Leaver",
                LEAVER,
                size = 60,
                events = listOf(event("keyPressed", moveInDirection(direction = 180, speed = 300), key = "L"), event("leftScene", mapOf("type" to "removeSelf"))),
            ),
            square("Trophy", TROPHY, size = 200),
        )

        /**
         * Where the lab Scene places its Actors: the Leaver wholly right of its bounds.
         */
        val labPlacements = listOf(
            Triple("Solid", SOLID_X, -120),
            Triple("Faded", FADED_X, -120),
            Triple("Ghost", 90, -120),
            Triple("Diver", 260, -180),
            Triple("Leaver", 650, 50),
            Triple("Paddle", -200, 200),
        )

        val referee = mapOf(
            "name" to "Referee",
            "events" to listOf(
                event("actorRemoved", log(GHOST_GONE), otherActorType = "Ghost"),
                event("actorRemoved", log(LEAVER_GONE), otherActorType = "Leaver"),
                event("actorAdded", log(BRICK_ADDED), otherActorType = "Brick"),
                event(
                    "actorRemoved",
                    log(BRICK_GONE),
                    mapOf("type" to "if", "condition" to mapOf("type" to "actorCount", "actorType" to "Brick", "comparison" to "equal", "value" to 0)),
                    mapOf("type" to "goToScene", "scene" to WIN_SCENE),
                    mapOf("type" to "end"),
                    otherActorType = "Brick",
                ),
                event("keyPressed", mapOf("type" to "goToScene", "scene" to SHORT_SCENE), key = "R"),
            ),
        )
    }
}

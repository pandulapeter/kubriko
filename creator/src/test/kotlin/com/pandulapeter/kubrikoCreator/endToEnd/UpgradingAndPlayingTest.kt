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
import com.pandulapeter.kubrikoCreator.template.ProjectUpgrader
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Breakout brought up to the latest template, then changed the way its upgrades make possible (its name, first Scene, aspect
 * ratio, and frame rate) along with a sprite and a Log Block, and played: what each change should do shows in the played
 * game, its console, and its build. Then a hand-written change reaches the running game, and a setting broken by hand fails
 * the build, naming it.
 */
class UpgradingAndPlayingTest {

    @Test
    fun anUpgradedBreakoutPlaysWithItsSettingsAndBreaksOnlyOverABrokenOne() = EndToEnd.scenario("upgrading-and-playing") {
        step("upgrade_project brings Breakout up to the latest template") {
            val upgrade = tools.call("get_project_upgrade")
            assertTrue(upgrade["isNeeded"]?.jsonPrimitive?.boolean == true, upgrade.text)
            val upgraded = tools.call("upgrade_project")
            assertFalse(upgraded.isError, upgraded.text)
            assertContains(File(project, ".kubriko/project.json").readText(), "\"templateVersion\": ${ProjectUpgrader.CURRENT_VERSION}")
        }
        step("A Scene of two sprite-drawn Gold Bricks, which log when they are placed, becomes the first one") {
            writeSprite(File(project, "src/commonMain/composeResources/drawable/$SPRITE_NAME.png"))
            // The resource scan that finds the new image runs within a second of it showing up.
            awaitSuccess("set_appearance", mapOf("actorType" to "GoldBrick", "sprite" to "drawable/$SPRITE_NAME.png", "width" to 60, "height" to 24))
            succeed("add_event", mapOf("actorType" to "GoldBrick", "type" to "addedToScene", "blocks" to listOf(mapOf("type" to "log", "message" to LOGGED_LINE))))
            succeed("create_scene", mapOf("scene" to BONUS_SCENE))
            succeed("place_actors", mapOf("scene" to BONUS_SCENE, "actors" to listOf(mapOf("actorType" to "GoldBrick", "x" to 0, "y" to 0), mapOf("actorType" to "GoldBrick", "x" to 60, "y" to 0))))
            succeed(
                "set_project_settings",
                mapOf("name" to GAME_NAME, "firstScene" to BONUS_SCENE, "aspectRatio" to "fixed", "viewportWidth" to VIEWPORT_WIDTH, "viewportHeight" to VIEWPORT_HEIGHT),
            )
        }
        step("start_game builds the game, which logs the Log Block's line") {
            val started = tools.call("start_game", timeout = 10.minutes)
            assertTrue(!started.isError && started["hasStarted"]?.jsonPrimitive?.boolean == true, started.text)
            awaitConsole(LOGGED_LINE)
        }
        step("The build names the game as gradle.properties does") {
            assertContains(File(project, "build/generated/gameInfo/GameInfo.kt").readText(), "const val NAME = \"$GAME_NAME\"")
        }
        val first = step("take_screenshot shows the game") { captureGame("bonus-scene") }
        step("The game starts in the bonus Scene, its Gold Bricks drawn as the sprite in the fixed viewport") {
            assertTrue(first.countOf(GOLD) == 0, "Gold Bricks were drawn as their color, not the sprite.")
            assertTrue(first.countOf(BRICK) == 0, "Bricks of the main Scene were drawn.")
            val gems = assertNotNull(first.boundsOf(SPRITE_COLOR), "No sprite was drawn.")
            // The viewport is VIEWPORT_WIDTH by VIEWPORT_HEIGHT units, as large as fits the game, in its middle.
            val scale = minOf(first.width / VIEWPORT_WIDTH, first.height / VIEWPORT_HEIGHT)
            assertTrue(abs(gems.width - 120 * scale) <= 3, "The sprites are ${gems.width} pixels wide, not ${120 * scale} (at $scale pixels a unit).")
            assertTrue(abs(gems.centerX - (first.width / 2 + 30 * scale)) <= 3, "The sprites are at $gems, not where the Scene puts them (at $scale pixels a unit).")
            val text = assertNotNull(first.boundsOf(TEXT), "The Game's texts weren't drawn.")
            val viewportLeft = (first.width - VIEWPORT_WIDTH * scale) / 2
            assertTrue(text.x >= viewportLeft - 2, "The texts start at ${text.x}, left of the viewport's edge at $viewportLeft.")
        }
        step("A change to Game.kt by hand reaches the running game") {
            val game = File(project, "src/commonMain/kotlin/com/example/breakout/Game.kt")
            val text = game.readText()
            val viewport = text.indexOf("KubrikoViewport(")
            val modifier = text.indexOf(VIEWPORT_MODIFIER, startIndex = viewport.coerceAtLeast(0))
            assertTrue(viewport >= 0 && modifier >= 0, "Game.kt has no KubrikoViewport with `$VIEWPORT_MODIFIER`:\n$text")
            val changed = text.replaceRange(modifier, modifier + VIEWPORT_MODIFIER.length, "modifier = modifier.background(Color(0xFF${"%06X".format(VIEWPORT_COLOR)})),")
            game.writeText(
                changed.replaceFirst(
                    "import androidx.compose.runtime.Composable",
                    "import androidx.compose.foundation.background\nimport androidx.compose.runtime.Composable\nimport androidx.compose.ui.graphics.Color",
                ),
            )
            // KubrikoViewport's modifier is the viewport's own, so the color fills it and leaves the bars beside it as they were.
            fun viewportPixel(image: BufferedImage) = image.getRGB(image.width / 2, image.height - 10)
            val reloaded = awaitGame("reloaded", timeout = 2.minutes) { ImageChecks.matches(viewportPixel(it), VIEWPORT_COLOR) }
            assertTrue(ImageChecks.matches(viewportPixel(reloaded), VIEWPORT_COLOR), "The viewport isn't the color Game.kt now gives it.")
            assertFalse(ImageChecks.matches(reloaded.getRGB(2, reloaded.height / 2), VIEWPORT_COLOR), "The bars beside the viewport took its color too.")
            assertTrue(reloaded.boundsOf(SPRITE_COLOR) != null, "The Gold Bricks are gone after the change.")
        }
        step("A frame rate broken by hand fails the build with a message naming it") {
            succeed("stop_game")
            val properties = File(project, "gradle.properties")
            val text = properties.readText()
            assertContains(text, "game.frameRate=60")
            properties.writeText(text.replace("game.frameRate=60", "game.frameRate=fast"))
            val started = tools.call("start_game", timeout = 5.minutes)
            assertTrue(started.isError || started["hasStarted"]?.jsonPrimitive?.boolean == false, started.text)
            assertContains(started.text, "game.frameRate in gradle.properties")
        }
        step("Choosing 60 again writes it") {
            succeed("set_project_settings", mapOf("frameRate" to 60))
            assertContains(File(project, "gradle.properties").readText(), "game.frameRate=60")
        }
    }

    private fun EndToEndScenario.succeed(tool: String, arguments: Map<String, Any?> = emptyMap()) = tools.call(tool, arguments).also { result ->
        assertFalse(result.isError, "$tool failed: ${result.text}")
    }

    private fun EndToEndScenario.awaitSuccess(tool: String, arguments: Map<String, Any?>, timeout: Duration = 15.seconds): ToolResult {
        val deadline = System.nanoTime() + timeout.inWholeNanoseconds
        while (true) {
            val result = tools.call(tool, arguments)
            if (!result.isError || System.nanoTime() >= deadline) return result.also { assertFalse(it.isError, "$tool failed: ${it.text}") }
            Thread.sleep(POLLING_INTERVAL_IN_MILLISECONDS)
        }
    }

    private fun EndToEndScenario.awaitConsole(line: String, timeout: Duration = 30.seconds) {
        val deadline = System.nanoTime() + timeout.inWholeNanoseconds
        while (true) {
            val console = tools.call("read_console").text
            if (line in console) return
            check(System.nanoTime() < deadline) { "The console never showed \"$line\":\n$console" }
            Thread.sleep(POLLING_INTERVAL_IN_MILLISECONDS)
        }
    }

    private fun EndToEndScenario.awaitGame(imageName: String, timeout: Duration, isReady: (BufferedImage) -> Boolean): BufferedImage {
        val deadline = System.nanoTime() + timeout.inWholeNanoseconds
        while (true) {
            val png = tools.call("take_screenshot").png
            val image = png?.let { ImageIO.read(it.inputStream()) }
            if ((image != null && isReady(image)) || System.nanoTime() >= deadline) return captureGame(imageName)
            Thread.sleep(POLLING_INTERVAL_IN_MILLISECONDS)
        }
    }

    private fun writeSprite(file: File) {
        file.parentFile.mkdirs()
        val image = BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            color = java.awt.Color(SPRITE_COLOR)
            fillRect(0, 0, SPRITE_SIZE, SPRITE_SIZE)
            dispose()
        }
        ImageIO.write(image, "png", file)
    }

    private companion object {
        const val GAME_NAME = "Breakout Deluxe"
        const val BONUS_SCENE = "bonus"
        const val SPRITE_NAME = "gem"
        const val SPRITE_SIZE = 16
        const val SPRITE_COLOR = 0x22AA22
        const val VIEWPORT_COLOR = 0xAA2244
        const val GOLD = 0xFFC857
        const val BRICK = 0x6060AA
        const val TEXT = 0x202020
        const val LOGGED_LINE = "A gem is in place."
        const val VIEWPORT_MODIFIER = "modifier = modifier,"
        const val POLLING_INTERVAL_IN_MILLISECONDS = 500L

        /**
         * Taller than any screen the checks run on, so that the fixed viewport fills the game's height and leaves bars beside it.
         */
        const val VIEWPORT_WIDTH = 1200f
        const val VIEWPORT_HEIGHT = 1800f
    }
}

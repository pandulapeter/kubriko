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
import com.pandulapeter.kubrikoCreator.testing.HeadlessChrome
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeTrue
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * Breakout brought up to the latest template, then changed the way its upgrades make possible (its name, first Scene, aspect
 * ratio, and frame rate) along with a sprite, a sprite sheet, and a Log Block, and played: what each change should do shows
 * in the played game, its console, and its build, and screenshots cropped to an Actor type (which the upgraded entry point
 * makes possible) frame its Actors and show the sheet's frame changing. Then a hand-written change reaches the running game,
 * a setting broken by hand fails the build, naming it, and the game played on the web draws in a headless browser, its
 * texts too, and takes in a change.
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
        step("A Scene of two sprite-drawn Gold Bricks, which log when they are placed, and a Blinker drawn as a sprite sheet becomes the first one") {
            writeSprite(File(project, "src/commonMain/composeResources/drawable/$SPRITE_NAME.png"), SPRITE_COLOR)
            // The resource scan that finds the new image runs within a second of it showing up.
            awaitSuccess("set_appearance", mapOf("actorType" to "GoldBrick", "sprite" to "drawable/$SPRITE_NAME.png", "width" to 60, "height" to 24))
            succeed("add_event", mapOf("actorType" to "GoldBrick", "type" to "addedToScene", "blocks" to listOf(mapOf("type" to "log", "message" to LOGGED_LINE))))
            succeed("create_scene", mapOf("scene" to BONUS_SCENE))
            addBlinker()
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
        step("take_screenshot cropped to the GoldBrick type shows both Gold Bricks in its middle, unscaled, with room around them") { checkGoldBrickCrop(first) }
        step("The Blinker plays its sprite sheet, its frame changing in screenshots cropped to it") { checkBlinkerFrames() }
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
            // A reload can create the game's Kubriko instance again, whose sprites then load a few frames after its first one.
            val reloaded = awaitGame("reloaded", timeout = 2.minutes) { ImageChecks.matches(viewportPixel(it), VIEWPORT_COLOR) && it.boundsOf(SPRITE_COLOR) != null }
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
        step("start_game on the web serves the game to the browser, out of the game tools' reach, texts and all, and a change reloads the page") {
            val chromePath = HeadlessChrome.find()
            // Last, so that a machine without Chrome still runs every other step.
            assumeTrue("Chrome isn't installed.", chromePath != null)
            assertContains(tools.call("list_play_targets").text, "\"id\":\"web\"")
            val started = tools.call("start_game", mapOf("target" to "web", "scene" to BONUS_SCENE), timeout = 10.minutes)
            assertTrue(!started.isError && started["hasStarted"]?.jsonPrimitive?.boolean == true, started.text)
            val address = URI(assertNotNull(Regex("""http://127\.0\.0\.1:\d+/\?[^\s"\\]*[^\s"\\.,;:)]""").find(started.text), "No address in: ${started.text}").value)
            assertContains(tools.call("press_keys", mapOf("keys" to listOf("SPACE"))).text, "runs on web")
            HeadlessChrome.start(checkNotNull(chromePath), outputDirectory, width = 1280, height = 800).use { chrome ->
                chrome.navigate(address)
                val page = awaitBrowser(chrome, "web-bonus-scene") { it.boundsOf(SPRITE_COLOR) != null && it.boundsOf(TEXT) != null }
                assertTrue(page.boundsOf(SPRITE_COLOR) != null, "The page doesn't draw the bonus Scene's sprites.")
                assertTrue(page.boundsOf(TEXT) != null, "The page doesn't draw the Game's texts.")
                val game = File(project, "src/commonMain/kotlin/com/example/breakout/Game.kt")
                val color = "Color(0xFF${"%06X".format(VIEWPORT_COLOR)})"
                assertContains(game.readText(), color)
                game.writeText(game.readText().replace(color, "Color(0xFF${"%06X".format(WEB_VIEWPORT_COLOR)})"))
                fun viewportPixel(image: BufferedImage) = image.getRGB(image.width / 2, image.height - 10)
                val reloaded = awaitBrowser(chrome, "web-reloaded", timeout = 5.minutes) { ImageChecks.matches(viewportPixel(it), WEB_VIEWPORT_COLOR) && it.boundsOf(SPRITE_COLOR) != null }
                assertTrue(ImageChecks.matches(viewportPixel(reloaded), WEB_VIEWPORT_COLOR), "The page didn't reload with the color Game.kt now gives the viewport.")
                assertTrue(reloaded.boundsOf(SPRITE_COLOR) != null, "The reloaded page doesn't draw the bonus Scene's sprites.")
            }
            succeed("stop_game")
        }
    }

    private fun EndToEndScenario.checkGoldBrickCrop(whole: BufferedImage) {
        val gems = assertNotNull(whole.boundsOf(SPRITE_COLOR))
        val crop = captureGame("bonus-scene-cropped", mapOf("actorType" to "GoldBrick"))
        assertTrue(crop.width < whole.width && crop.height < whole.height, "The crop is ${crop.width}×${crop.height}, as large as the game's ${whole.width}×${whole.height}.")
        val cropped = assertNotNull(crop.boundsOf(SPRITE_COLOR), "The crop doesn't show the Gold Bricks.")
        assertTrue(abs(cropped.width - gems.width) <= 2 && abs(cropped.height - gems.height) <= 2, "The crop is scaled: the Gold Bricks are $cropped in it and $gems in the whole game.")
        assertTrue(cropped.x > 0 && cropped.y > 0 && cropped.maxX < crop.width && cropped.maxY < crop.height, "The crop cuts the Gold Bricks, at $cropped in ${crop.width}×${crop.height}.")
        assertTrue(
            abs(cropped.centerX - crop.width / 2.0) <= 3 && abs(cropped.centerY - crop.height / 2.0) <= 3,
            "The Gold Bricks are at $cropped, not in the middle of ${crop.width}×${crop.height}.",
        )
        // Half of their size is left around them on every side, unless a twentieth of the view is more.
        assertTrue(crop.width >= gems.width * 2 - 2 && crop.height >= gems.height * 2 - 2, "There is too little room around the Gold Bricks, at $cropped in ${crop.width}×${crop.height}.")
    }

    /**
     * Adds a Blinker type drawn as a sprite sheet of two frames, each one color, below the bonus Scene's Gold Bricks.
     */
    private fun EndToEndScenario.addBlinker() {
        writeSprite(File(project, "src/commonMain/composeResources/drawable/$SHEET_NAME.png"), *FRAME_COLORS)
        val sheet = mapOf("frameWidth" to SPRITE_SIZE, "frameHeight" to SPRITE_SIZE, "frameCount" to FRAME_COLORS.size, "framesPerRow" to FRAME_COLORS.size, "framesPerSecond" to 2)
        awaitSuccess("set_sprite_sheet", mapOf("sprite" to "drawable/$SHEET_NAME.png", "sheet" to sheet))
        succeed("create_actor_type", mapOf("name" to "Blinker", "shape" to "rectangle", "sprite" to "drawable/$SHEET_NAME.png", "width" to 80, "height" to 80))
        succeed("place_actors", mapOf("scene" to BONUS_SCENE, "actors" to listOf(mapOf("actorType" to "Blinker", "x" to 0, "y" to 300))))
    }

    private fun EndToEndScenario.checkBlinkerFrames() {
        val frames = FRAME_COLORS.mapIndexed { index, color ->
            fun showsOnlyThisFrame(image: BufferedImage) = image.countOf(color) > 0 && FRAME_COLORS.none { it != color && image.countOf(it) > 0 }
            val frame = awaitGame("blinker-frame-${index + 1}", arguments = mapOf("actorType" to "Blinker"), isReady = ::showsOnlyThisFrame)
            assertTrue(showsOnlyThisFrame(frame), "The Blinker never showed only its frame ${index + 1}, #%06X.".format(color))
            frame
        }
        val differing = ImageChecks.differenceBetween(frames[0], frames[1]) * frames[0].width * frames[0].height
        val blinker = frames[0].countOf(FRAME_COLORS[0])
        assertTrue(differing >= blinker * 0.9f, "Only $differing pixels differ between the Blinker's two frames, of the $blinker it covers.")
    }

    /**
     * Writes a sprite of square frames side by side, each one of [frameColors].
     */
    private fun writeSprite(file: File, vararg frameColors: Int) {
        file.parentFile.mkdirs()
        val image = BufferedImage(SPRITE_SIZE * frameColors.size, SPRITE_SIZE, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            frameColors.forEachIndexed { index, frameColor ->
                color = java.awt.Color(frameColor)
                fillRect(index * SPRITE_SIZE, 0, SPRITE_SIZE, SPRITE_SIZE)
            }
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
        const val SHEET_NAME = "blink"
        val FRAME_COLORS = intArrayOf(0xCC22CC, 0x22CCCC)
        const val VIEWPORT_COLOR = 0xAA2244
        const val WEB_VIEWPORT_COLOR = 0x2244AA
        const val GOLD = 0xFFC857
        const val BRICK = 0x6060AA
        const val TEXT = 0x202020
        const val LOGGED_LINE = "A gem is in place."
        const val VIEWPORT_MODIFIER = "modifier = modifier,"

        /**
         * Taller than any screen the checks run on, so that the fixed viewport fills the game's height and leaves bars beside it.
         */
        const val VIEWPORT_WIDTH = 1200f
        const val VIEWPORT_HEIGHT = 1800f
    }
}

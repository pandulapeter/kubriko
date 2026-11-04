/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(EditorMutation::class)

package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.actors.codegen.SceneSettingsCodeGenerator
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeStore
import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFiles
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import com.pandulapeter.kubrikoCreator.session.WebGameServer
import com.pandulapeter.kubrikoCreator.testing.HeadlessChrome
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import org.junit.Assume.assumeTrue
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Builds the starter template's web distribution for real, serves it as Kubriko Creator does, and looks at it in headless
 * Chrome (or Chromium): that the page draws the Scene its address names and a Manager type's text in a fixed viewport,
 * even with the language a browser started under the POSIX locale reports, and that a change, once built, reloads the page
 * by itself. Runs with `KUBRIKO_CREATOR_INTEGRATION=true` where Chrome is found ([HeadlessChrome.find]), opens no window,
 * and leaves what Chrome showed, and the project, in `build/web-game`.
 */
class WebGameIntegrationTest {
    private val output = File("build/web-game")

    @Test
    fun theServedPageDrawsTheSceneItsAddressNamesAndReloadsOnceAChangeIsBuilt() {
        assumeTrue(System.getenv("KUBRIKO_CREATOR_INTEGRATION") == "true")
        val chromePath = HeadlessChrome.find()
        assumeTrue("Chrome isn't installed.", chromePath != null)
        val parentDirectory = File(output, "project").apply { deleteRecursively() }
        val configuration = configuration()
        val directory = ProjectGenerator(configuration).create(NewProjectRequest(name = "Web Game", packageName = "com.example.web", parentDirectory = parentDirectory))
        checkNotNull(BlockTypeStore.open(directory)).apply {
            save(ManagerTypeDefinition(name = "Hud", texts = listOf(ScreenTextDefinition(text = "WWWWWW", size = 64f, color = TEXT))))
            regenerateAll()
        }
        KubrikoBuildFiles(directory, configuration.kubrikoVersion, configuration.useMavenLocal).sync()
        // A fixed aspect ratio, which letterboxes the viewport, is what an entry point sizing the game itself broke.
        File(directory, "gradle.properties").apply {
            writeText(readText().replace("game.aspectRatio=dynamic", "game.aspectRatio=fixed").replace("game.viewportWidth=1920", "game.viewportWidth=1200").replace("game.viewportHeight=1080", "game.viewportHeight=1800"))
        }
        val scenes = File(directory, "src/commonMain/composeResources/files/scenes")
        File(scenes, "main.json").writeText(sceneColored(RED))
        val colored = File(scenes, "colored.json").apply { writeText(sceneColored(GREEN)) }
        GradleProjectConnection(directory).use { connection ->
            build(connection)
            runBlocking { WebGameServer.start(File(directory, DISTRIBUTION)) }.use { server ->
                HeadlessChrome.start(checkNotNull(chromePath), parentDirectory, width = 800, height = 600).use { chrome ->
                    // Compose draws no text under a language that isn't a tag, which the web Main.kt guards against.
                    chrome.reportLanguages("en-US@posix")
                    chrome.navigate(server.addressOf("colored"))
                    chrome.awaitColor(GREEN, "colored")
                    chrome.awaitText()
                    chrome.navigate(server.addressOf(null))
                    chrome.awaitColor(RED, "first")
                    chrome.navigate(server.addressOf("colored"))
                    chrome.awaitColor(GREEN, "colored-again")
                    colored.writeText(sceneColored(BLUE))
                    build(connection)
                    server.reload()
                    chrome.awaitColor(BLUE, "reloaded")
                }
            }
        }
    }

    private fun build(connection: GradleProjectConnection) {
        val log = StringBuilder()
        val isSuccessful = runBlocking { connection.run(taskPath = ":wasmJsBrowserDevelopmentExecutableDistribution", arguments = emptyList(), onEvent = { log.appendLine(it.toString()) }) }
        assertTrue(isSuccessful, "The web distribution doesn't build:\n$log")
    }

    private fun sceneColored(color: Long) = buildJsonArray {
        addJsonObject {
            put("typeId", SceneSettingsCodeGenerator.SETTINGS_NAME)
            put("state", SceneSettingsState.withChanges(backgroundColor = color, width = 4000f, height = 4000f))
        }
    }.toString()

    /**
     * Waits until the middle of the page shows [expected], saving what it showed last as `build/web-game/<name>.png`.
     */
    private fun HeadlessChrome.awaitColor(expected: Long, name: String) {
        val image = awaitScreenshot(LOAD_TIME_IN_MILLISECONDS) { isClose(it.centerPixel, expected) } ?: fail("$name could never be captured.")
        ImageIO.write(image, "png", File(output.apply { mkdirs() }, "$name.png"))
        File(output, "console.txt").writeText(console.joinToString("\n"))
        if (!isClose(image.centerPixel, expected)) fail("$name shows ${Integer.toHexString(image.centerPixel)}, not ${java.lang.Long.toHexString(expected)}.")
    }

    /**
     * Waits until the Manager type's text shows in the top left quarter of the page, saving it as `build/web-game/text.png`.
     */
    private fun HeadlessChrome.awaitText() {
        val image = awaitScreenshot(LOAD_TIME_IN_MILLISECONDS) { it.textPixels > 0 } ?: fail("The text could never be captured.")
        ImageIO.write(image, "png", File(output.apply { mkdirs() }, "text.png"))
        if (image.textPixels == 0) fail("The Manager type's text isn't drawn.")
    }

    private val BufferedImage.textPixels get() = (0 until width / 2).sumOf { x -> (0 until height / 2).count { y -> isClose(getRGB(x, y), TEXT) } }

    private val BufferedImage.centerPixel get() = getRGB(width / 2, height / 2)

    private fun isClose(actual: Int, expected: Long) = listOf(16, 8, 0).all { shift -> abs((actual shr shift and 0xFF) - (expected shr shift and 0xFF).toInt()) <= 8 }

    private fun configuration(): CreatorConfiguration {
        val bundled = CreatorConfiguration.load()
        return System.getenv("KUBRIKO_CREATOR_VERSION")?.let { bundled.copy(kubrikoVersion = it, useMavenLocal = true) } ?: bundled
    }

    private companion object {
        const val DISTRIBUTION = "build/dist/wasmJs/developmentExecutable"
        const val LOAD_TIME_IN_MILLISECONDS = 60_000L
        const val RED = 0xFFC00000L
        const val GREEN = 0xFF00C000L
        const val BLUE = 0xFF0000C0L
        const val TEXT = 0xFFFFE000L
    }
}

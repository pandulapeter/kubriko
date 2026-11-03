/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.actors.codegen.SceneSettingsCodeGenerator
import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
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
 * Chrome (or Chromium): that the page draws the Scene its address names, and that a change, once built, reloads the page
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
        val directory = ProjectGenerator(configuration()).create(NewProjectRequest(name = "Web Game", packageName = "com.example.web", parentDirectory = parentDirectory))
        val scenes = File(directory, "src/commonMain/composeResources/files/scenes")
        File(scenes, "main.json").writeText(sceneColored(RED))
        val colored = File(scenes, "colored.json").apply { writeText(sceneColored(GREEN)) }
        GradleProjectConnection(directory).use { connection ->
            build(connection)
            runBlocking { WebGameServer.start(File(directory, DISTRIBUTION)) }.use { server ->
                HeadlessChrome.start(checkNotNull(chromePath), parentDirectory, width = 800, height = 600).use { chrome ->
                    chrome.navigate(server.addressOf("colored"))
                    chrome.awaitColor(GREEN, "colored")
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
        val end = System.currentTimeMillis() + LOAD_TIME_IN_MILLISECONDS
        var image: BufferedImage? = null
        do {
            // A page that is navigating or reloading has nothing to capture for a moment.
            image = runCatching { screenshot() }.getOrNull() ?: image
            if (image != null && isClose(image.getRGB(image.width / 2, image.height / 2), expected)) break
            Thread.sleep(250)
        } while (System.currentTimeMillis() < end)
        File(output, "console.txt").writeText(console.joinToString("\n"))
        if (image == null) fail("$name could never be captured.")
        ImageIO.write(image, "png", File(output.apply { mkdirs() }, "$name.png"))
        val actual = image.getRGB(image.width / 2, image.height / 2)
        if (!isClose(actual, expected)) fail("$name shows ${Integer.toHexString(actual)}, not ${java.lang.Long.toHexString(expected)}.")
    }

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
    }
}

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

import com.pandulapeter.kubrikoCreator.testing.HeadlessChrome
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * One end-to-end scenario: the real Kubriko Creator on a fresh copy of [project], driven through [tools] and judged by
 * what it left in the project and on the screen. Every image it takes is saved in [outputDirectory], numbered in order,
 * so that whoever runs it can look at them, and a failure names its step, the last images, and the end of Kubriko
 * Creator's log, which [logTail] reads.
 */
internal class EndToEndScenario(
    val name: String,
    val project: File,
    val outputDirectory: File,
    val tools: CreatorTools,
    private val logTail: () -> String,
) {
    private val images = mutableListOf<File>()

    /**
     * Runs [block] as the step [description], so that its failure says which step it was.
     */
    fun <T> step(description: String, block: () -> T): T = try {
        block()
    } catch (problem: Throwable) {
        throw ScenarioFailure.of(name, description, problem, images.takeLast(LAST_IMAGES), logTail())
    }

    /**
     * The whole screen, with Kubriko Creator's window and the game's, saved as [imageName].
     */
    fun captureScreen(imageName: String): BufferedImage = awaitScreen(imageName, timeout = Duration.ZERO) { true }

    /**
     * The whole screen once [isReady] accepts it, or as it is after [timeout], saved as [imageName]: what a window shows
     * reaches the screen a few frames after the change that caused it.
     */
    fun awaitScreen(imageName: String, timeout: Duration = SCREEN_TIMEOUT, isReady: (BufferedImage) -> Boolean): BufferedImage {
        val robot = Robot()
        val deadline = System.nanoTime() + timeout.inWholeNanoseconds
        while (true) {
            val screen = robot.createScreenCapture(Rectangle(Toolkit.getDefaultToolkit().screenSize))
            if (isReady(screen) || System.nanoTime() >= deadline) return screen.also { save(imageName, it) }
            Thread.sleep(SCREEN_POLLING_INTERVAL_IN_MILLISECONDS)
        }
    }

    /**
     * What the running game shows, through `take_screenshot` (the game's own drawing, wherever its window is), saved as
     * [imageName].
     */
    fun captureGame(imageName: String): BufferedImage {
        val result = tools.call("take_screenshot")
        check(!result.isError) { "take_screenshot failed: ${result.text}" }
        val png = checkNotNull(result.png) { "take_screenshot returned no image: ${result.text}" }
        return checkNotNull(ImageIO.read(ByteArrayInputStream(png))) { "take_screenshot's image can't be read." }.also { save(imageName, it) }
    }

    /**
     * What [chrome]'s page shows once [isReady] accepts it, or as it is after [timeout], saved as [imageName].
     */
    fun awaitBrowser(chrome: HeadlessChrome, imageName: String, timeout: Duration = BROWSER_TIMEOUT, isReady: (BufferedImage) -> Boolean): BufferedImage =
        checkNotNull(chrome.awaitScreenshot(timeout.inWholeMilliseconds, isReady)) { "The browser's page could never be captured." }.also { save(imageName, it) }

    private fun save(imageName: String, image: BufferedImage) {
        val file = File(outputDirectory, "%02d-%s.png".format(images.size + 1, imageName))
        ImageIO.write(image, "png", file)
        images += file
    }

    private companion object {
        const val LAST_IMAGES = 3
        const val SCREEN_POLLING_INTERVAL_IN_MILLISECONDS = 250L
        val SCREEN_TIMEOUT = 15.seconds
        val BROWSER_TIMEOUT = 60.seconds
    }
}

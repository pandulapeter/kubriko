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
import kotlin.test.assertFalse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
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
        return poll(timeout, SCREEN_POLLING_INTERVAL) { remaining ->
            robot.createScreenCapture(Rectangle(Toolkit.getDefaultToolkit().screenSize)).takeIf { !remaining.isPositive() || isReady(it) }
        }.also { save(imageName, it) }
    }

    /**
     * What the running game shows, through `take_screenshot` (the game's own drawing, wherever its window is), saved as
     * [imageName].
     */
    fun captureGame(imageName: String, timeout: Duration = CreatorTools.DEFAULT_TIMEOUT): BufferedImage {
        val result = tools.call("take_screenshot", timeout = timeout)
        check(!result.isError) { "take_screenshot failed: ${result.text}" }
        val png = checkNotNull(result.png) { "take_screenshot returned no image: ${result.text}" }
        return checkNotNull(decode(png)) { "take_screenshot's image can't be read." }.also { save(imageName, it) }
    }

    /**
     * What the running game shows once [isReady] accepts it, saved as [imageName], or as it is after [timeout]. Until
     * then a screenshot that fails, throws, or can't be read counts as not ready, since a game restarting shows nothing
     * for a while; the last one says why.
     */
    fun awaitGame(imageName: String, timeout: Duration = GAME_TIMEOUT, isReady: (BufferedImage) -> Boolean): BufferedImage = poll(timeout, CreatorTools.POLLING_INTERVAL) { remaining ->
        if (!remaining.isPositive()) {
            captureGame(imageName)
        } else {
            callForWait("take_screenshot", emptyMap(), remaining)?.png?.let(::decode)?.takeIf(isReady)?.also { save(imageName, it) }
        }
    }

    /**
     * Calls [tool] with [arguments], failing the step when it fails.
     */
    fun succeed(tool: String, arguments: Map<String, Any?> = emptyMap()) = tools.call(tool, arguments).also { result ->
        assertFalse(result.isError, "$tool failed: ${result.text}")
    }

    /**
     * Calls [tool] with [arguments] until it succeeds, failing the step when it still fails after [timeout].
     */
    fun awaitSuccess(tool: String, arguments: Map<String, Any?>, timeout: Duration = 15.seconds): ToolResult = poll(timeout, CreatorTools.POLLING_INTERVAL) { remaining ->
        callForWait(tool, arguments, remaining)?.takeIf { !remaining.isPositive() || !it.isError }
    }.also { assertFalse(it.isError, "$tool failed: ${it.text}") }

    /**
     * Waits until Kubriko Creator's console shows [line], failing the step when it doesn't within [timeout].
     */
    fun awaitConsole(line: String, timeout: Duration = 30.seconds) {
        poll(timeout, CreatorTools.POLLING_INTERVAL) { remaining ->
            val console = callForWait("read_console", emptyMap(), remaining)?.text.orEmpty()
            when {
                line in console -> true
                !remaining.isPositive() -> error("The console never showed \"$line\":\n$console")
                else -> null
            }
        }
    }

    /**
     * What [chrome]'s page shows once [isReady] accepts it, or as it is after [timeout], saved as [imageName] with what the
     * page logged and threw beside it, which is all there is to tell why a page that never gets ready doesn't.
     */
    fun awaitBrowser(chrome: HeadlessChrome, imageName: String, timeout: Duration = BROWSER_TIMEOUT, isReady: (BufferedImage) -> Boolean): BufferedImage =
        checkNotNull(chrome.awaitScreenshot(timeout.inWholeMilliseconds, isReady)) { "The browser's page could never be captured." }.also { image ->
            save(imageName, image)
            File(outputDirectory, "%02d-%s-console.txt".format(images.size, imageName)).writeText(chrome.console.joinToString("\n"))
        }

    /**
     * What [attempt] returns once it returns something, trying again every [interval] until [timeout] has passed. Each
     * attempt is told how much of the timeout is left; the last, told none is, returns or throws. A wait outlasts its
     * timeout by at most one call of [MINIMUM_CALL_TIMEOUT] and its last call ([callForWait]).
     */
    private inline fun <T : Any> poll(timeout: Duration, interval: Duration, attempt: (remaining: Duration) -> T?): T {
        val deadline = System.nanoTime() + timeout.inWholeNanoseconds
        while (true) {
            val remaining = (deadline - System.nanoTime()).nanoseconds
            attempt(remaining)?.let { return it }
            check(remaining.isPositive()) { "The last attempt returned nothing." }
            Thread.sleep(interval.inWholeMilliseconds)
        }
    }

    /**
     * What [tool] answers within what is [remaining] of a wait (at least [MINIMUM_CALL_TIMEOUT]), or null when the call
     * fails on the way, which a wait takes for not ready yet; an interrupt never is. The last call, once nothing is
     * left, may take as long as any call and throws what it meets, so the wait says why it gave up.
     */
    private fun callForWait(tool: String, arguments: Map<String, Any?>, remaining: Duration): ToolResult? {
        if (!remaining.isPositive()) return tools.call(tool, arguments)
        return try {
            tools.call(tool, arguments, timeout = remaining.coerceIn(MINIMUM_CALL_TIMEOUT, CreatorTools.DEFAULT_TIMEOUT))
        } catch (exception: InterruptedException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }

    private fun decode(png: ByteArray): BufferedImage? = try {
        ImageIO.read(ByteArrayInputStream(png))
    } catch (_: Exception) {
        // ImageIO throws unchecked exceptions too on a malformed image.
        null
    }

    private fun save(imageName: String, image: BufferedImage) {
        val file = File(outputDirectory, "%02d-%s.png".format(images.size + 1, imageName))
        ImageIO.write(image, "png", file)
        images += file
    }

    private companion object {
        const val LAST_IMAGES = 3
        val SCREEN_POLLING_INTERVAL = 250.milliseconds
        val SCREEN_TIMEOUT = 15.seconds
        val GAME_TIMEOUT = 30.seconds
        val MINIMUM_CALL_TIMEOUT = 10.seconds
        val BROWSER_TIMEOUT = 60.seconds
    }
}

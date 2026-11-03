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

import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

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
    fun captureScreen(imageName: String): BufferedImage = Robot()
        .createScreenCapture(Rectangle(Toolkit.getDefaultToolkit().screenSize))
        .also { save(imageName, it) }

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

    private fun save(imageName: String, image: BufferedImage) {
        val file = File(outputDirectory, "%02d-%s.png".format(images.size + 1, imageName))
        ImageIO.write(image, "png", file)
        images += file
    }

    private companion object {
        const val LAST_IMAGES = 3
    }
}

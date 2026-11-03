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

import com.pandulapeter.kubrikoCreator.commands.FakeProjectRunner
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.test.runTest
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class EndToEndScenarioTest {
    private val outputDirectory = testDirectory()

    private fun ServedProject.scenario() = EndToEndScenario("a scenario", project = testDirectory(), outputDirectory, tools, logTail = { "Last line." })

    @Test
    fun aStepReturnsWhatItsBlockDoes() = runTest {
        withServedTools {
            assertEquals(42, scenario().step("Answer") { 42 })
        }
    }

    @Test
    fun aFailedStepNamesItselfTheLastImagesAndTheLog() = runTest {
        val png = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", it) }.toByteArray()
        withServedTools(FakeProjectRunner(step = GameStep.Done(console = emptyList(), png = png))) {
            open()
            val scenario = scenario()
            repeat(4) { index -> scenario.captureGame("frame-$index") }
            assertEquals(listOf("01-frame-0.png", "02-frame-1.png", "03-frame-2.png", "04-frame-3.png"), outputDirectory.list()!!.sorted())
            val problem = IllegalStateException("The Ball is missing.")
            val failure = assertFailsWith<ScenarioFailure> { scenario.step("The game draws the Ball") { throw problem } }
            assertSame(problem, failure.cause)
            val images = listOf("02-frame-1.png", "03-frame-2.png", "04-frame-3.png").joinToString { File(outputDirectory, it).path }
            assertEquals(
                "\"a scenario\" failed at \"The game draws the Ball\": The Ball is missing.\nThe last images: $images\nThe end of Kubriko Creator's log:\nLast line.",
                failure.message,
            )
        }
    }

    @Test
    fun aStepFailingBeforeAnyImageSaysSo() = runTest {
        withServedTools {
            val failure = assertFailsWith<ScenarioFailure> { scenario().step("Opening") { error("Too slow.") } }
            assertEquals("\"a scenario\" failed at \"Opening\": Too slow.\nNo images were taken before it.\nThe end of Kubriko Creator's log:\nLast line.", failure.message)
        }
    }

    @Test
    fun aGameThatCantBeCapturedFailsTheStep() = runTest {
        withServedTools(FakeProjectRunner(step = GameStep.NotPlaying)) {
            open()
            val scenario = scenario()
            assertFailsWith<ScenarioFailure> { scenario.step("Capturing") { scenario.captureGame("game") } }
            assertEquals(emptyList(), outputDirectory.list()!!.toList())
        }
    }
}

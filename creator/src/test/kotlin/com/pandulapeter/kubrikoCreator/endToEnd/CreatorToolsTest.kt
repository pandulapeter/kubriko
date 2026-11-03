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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreatorToolsTest {

    @Test
    fun theSeededSettingsTurnTheServerOnAndTheWaitEndsOnceAProjectIsOpen() = runTest {
        withServedTools {
            var probes = 0
            tools.awaitOpenProject {
                probes++
                if (probes == 3) open()
                true
            }
            assertTrue(probes >= 3, "The wait ended after $probes probes, before the project was open.")
            val overview = tools.call("get_project_overview")
            assertFalse(overview.isError, overview.text)
            assertEquals(JsonPrimitive("game"), overview["name"])
            assertNull(overview.png)
        }
    }

    @Test
    fun theWaitStopsWithTheLastProblemWhenCreatorQuits() = runTest {
        withServedTools {
            var probes = 0
            val failure = assertFailsWith<IllegalStateException> {
                tools.awaitOpenProject {
                    probes++
                    probes < 6
                }
            }
            assertEquals("Kubriko Creator quit before a project was open. No project is open in Kubriko Creator.", failure.message)
        }
    }

    @Test
    fun aToolsErrorAndImageComeBack() = runTest {
        val red = BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB).apply { for (x in 0 until 4) for (y in 0 until 2) setRGB(x, y, 0xFF0000) }
        val png = ByteArrayOutputStream().also { ImageIO.write(red, "png", it) }.toByteArray()
        withServedTools(FakeProjectRunner(step = GameStep.Done(console = emptyList(), png = png))) {
            open()
            val screenshot = tools.call("take_screenshot")
            assertFalse(screenshot.isError, screenshot.text)
            val image = ImageIO.read(ByteArrayInputStream(assertNotNull(screenshot.png)))
            assertEquals(0xFF0000, image.getRGB(0, 0) and 0xFFFFFF)
            val rejected = tools.call("start_game", mapOf("scene" to "nowhere"))
            assertTrue(rejected.isError)
            assertContains(rejected.text, "nowhere")
        }
    }
}

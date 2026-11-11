/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetAppearance
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.testing.uiTest
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AppearanceColumnTest {
    private val image = File(testDirectory(), "hero.png").apply {
        val pixels = BufferedImage(32, 8, BufferedImage.TYPE_INT_ARGB)
        listOf(Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE).forEachIndexed { index, color ->
            (0 until 8).forEach { x -> (0 until 8).forEach { y -> pixels.setRGB(index * 8 + x, y, color.rgb) } }
        }
        ImageIO.write(pixels, "png", this)
    }
    private val hero = ActorTypeDefinition(name = "Hero", sprite = "drawable/hero.png")
    private val sheet = SpriteSheet(frameWidth = 8, frameHeight = 8, frameCount = 4, framesPerRow = 4, framesPerSecond = 6f)

    private fun sprites(sheets: Map<String, SpriteSheet> = emptyMap()) = SpriteChoices(SpriteFiles(mapOf("drawable/hero.png" to image)), sheets)

    @Test
    fun aSpriteShowsHowItIsCutAndItsColorOnlyWhileItIsTinted() = uiTest {
        var definition by mutableStateOf(hero)
        val changes = mutableListOf<SetAppearance.Input>()
        showing {
            AppearanceColumn(
                definition = definition,
                sprites = sprites(mapOf("hero" to sheet)),
                onChange = { changes += it },
                onSheetChange = { _, _ -> },
            )
        }
        onNodeWithText("Frames: 4, at 6 a second").assertExists()
        onAllNodes(hasText("#", substring = true)).assertCountEquals(0)
        onNode(isToggleable() and hasText("Tint with the color")).performClick()
        assertEquals(listOf(SetAppearance.Input("Hero", tintsSprite = true)), changes)
        definition = hero.copy(tintsSprite = true)
        waitForIdle()
        saveSnapshot("appearance-sprite-sheet-tinted")
    }

    @Test
    fun aSpriteIsCutIntoFramesGuessedFromItsImageAndATypeAsLargeAsItBecomesAFrame() = uiTest(width = 800, height = 700) {
        val saved = mutableListOf<SpriteSheet?>()
        val changes = mutableListOf<SetAppearance.Input>()
        showing {
            AppearanceColumn(
                definition = hero.copy(width = 32f, height = 8f),
                sprites = sprites(),
                onChange = { changes += it },
                onSheetChange = { path, sheet ->
                    assertEquals("drawable/hero.png", path)
                    saved += sheet
                },
            )
        }
        onNodeWithText("The whole image").assertExists()
        onNodeWithText("Frames…").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("The image is 32 by 8 pixels", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Frames of hero.png").assertExists()
        saveSnapshot("sprite-sheet-dialog")
        onNodeWithText("Save").performClick()
        assertEquals(listOf<SpriteSheet?>(SpriteSheet(frameWidth = 8, frameHeight = 8, frameCount = 4, framesPerRow = 4)), saved)
        assertEquals(listOf(SetAppearance.Input("Hero", width = 8f, height = 8f)), changes)
    }

    @Test
    fun framesThatDontFitTheImageCantBeSavedAndACutSpriteCanBeShownWholeAgain() = uiTest(width = 800, height = 700) {
        val saved = mutableListOf<SpriteSheet?>()
        showing {
            AppearanceColumn(
                definition = hero,
                sprites = sprites(mapOf("hero" to sheet.copy(frameCount = 5))),
                onChange = {},
                onSheetChange = { _, sheet -> saved += sheet },
            )
        }
        onNodeWithText("Frames…").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("The frames don't fit the image", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Save").performClick()
        assertEquals(emptyList(), saved)
        onNodeWithText("Show the whole image").performClick()
        assertEquals(listOf<SpriteSheet?>(null), saved)
    }
}

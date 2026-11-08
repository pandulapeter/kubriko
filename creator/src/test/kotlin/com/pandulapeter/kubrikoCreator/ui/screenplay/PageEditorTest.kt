/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.rightClick
import com.pandulapeter.kubrikoCreator.screenplay.Mention
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.assistant.LocalWritersRoom
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class PageEditorTest {
    private val page = "# The hero\n\nA knight who jumps between **floating** islands.\n\n## Moves\n\n- Runs\n- Jumps\n\n## Planned\n\n- Wall jump\n"

    @Test
    fun aPageShowsRenderedAndAChunkClickedIsEditedInPlace() = uiTest(width = 900, height = 640) {
        val saved = mutableListOf<String>()
        showing { PageEditor(name = "hero", text = page, onTextChange = { saved += it }) }
        onNodeWithText("The hero").assertExists()
        saveSnapshot("page-editor")
        onNodeWithText("Moves").performClick()
        waitForIdle()
        onNodeWithText("## Moves").assertExists()
        onNodeWithText("Bold").assertExists()
        saveSnapshot("page-editor-editing")
        assertEquals(emptyList(), saved)
    }

    @Test
    fun anEmptyPageSaysHowToStartIt() = uiTest(width = 700, height = 300) {
        showing { PageEditor(name = "villain", text = "", onTextChange = {}) }
        onNodeWithText("This page is empty. Add a paragraph to start it.").assertExists()
        onNodeWithText("Add a paragraph").performClick()
        waitForIdle()
        onNodeWithText("Heading").assertExists()
    }

    @Test
    fun mentionsAreChipsThatOpenWhatTheyNameAndWebAddressesAreShownFirst() = uiTest(width = 900, height = 400) {
        val opened = mutableListOf<Mention>()
        val links = PageLinksState("screenplay/README.md", File(".")) { opened += it }
        links.looks = mapOf(
            "../.kubriko/actors/Player.json" to MentionLook(Mention.ActorType("Player", ".kubriko/actors/Player.json"), exists = true),
            "villain.md" to MentionLook(Mention.Page("villain", "screenplay/villain.md"), exists = false),
        )
        val text = "# The game\n\nThe [hero](../.kubriko/actors/Player.json) meets the [villain](villain.md).\n\n[Read more](https://example.com)\n"
        showing { PageEditor(name = "README", text = text, onTextChange = {}, links = links) }
        onNodeWithText("The hero meets the villain.").assertExists()
        saveSnapshot("page-editor-mentions")
        onNodeWithText("hero").performClick()
        waitForIdle()
        assertEquals(listOf<Mention>(Mention.ActorType("Player", ".kubriko/actors/Player.json")), opened)
        onNodeWithText("Bold").assertDoesNotExist()
        onNodeWithText("Read more").performTouchInput { click(Offset(8f, centerY)) }
        waitForIdle()
        onNodeWithText("https://example.com").assertExists()
        saveSnapshot("page-editor-address")
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        assertNull(links.pendingAddress)
    }

    @Test
    fun typingAnAtSignOffersWhatThePageCanMentionAndEnterWritesTheLink() = uiTest(width = 900, height = 500) {
        val links = PageLinksState("screenplay/hero.md", File(".")) {}
        links.candidates = listOf("Ball", "Paddle", "Brick").map { MentionCandidate(it, null, Mention.ActorType(it, ".kubriko/actors/$it.json")) }
        showing { PageEditor(name = "hero", text = page, onTextChange = {}, links = links) }
        onNodeWithText("Moves").performClick()
        waitForIdle()
        onNode(hasSetTextAction()).performTextInput(" @b")
        waitForIdle()
        onNodeWithText("Ball").assertExists()
        onNodeWithText("Brick").assertExists()
        onNodeWithText("Paddle").assertDoesNotExist()
        saveSnapshot("page-editor-mention-suggestions")
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.DirectionDown) }
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        onNodeWithText("## Moves [Brick](../.kubriko/actors/Brick.json)").assertExists()
        onNodeWithText("Ball").assertDoesNotExist()
        onNode(hasSetTextAction()).performTextInput(" @b")
        waitForIdle()
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        onNodeWithText("Ball").assertDoesNotExist()
        onNode(hasSetTextAction()).performKeyInput {
            pressKey(Key.Backspace)
            pressKey(Key.Backspace)
        }
        onNode(hasSetTextAction()).performTextInput("@b")
        waitForIdle()
        onNodeWithText("Ball").assertExists()
    }

    @Test
    fun aPageMovedWhileItIsTypedIntoKeepsTheTypingAndSavesUnderItsNewName() = uiTest(width = 900, height = 500) {
        val saved = mutableListOf<Pair<String, String>>()
        var name by mutableStateOf("hero")
        showing { PageEditor(name = name, text = page, onTextChange = { saved += name to it }, identity = "the same page") }
        onNodeWithText("Moves").performClick()
        waitForIdle()
        onNode(hasSetTextAction()).performTextInput(" fast")
        name = "cast/hero"
        waitForIdle()
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        assertEquals(listOf("cast/hero"), saved.map { it.first }.distinct())
        assertEquals(page.replace("## Moves", "## Moves fast"), saved.last().second)
    }

    @Test
    fun imagesShowOnThePageAndTheToolbarPutsASpriteAfterTheChunkTypedInto() = uiTest(width = 900, height = 520) {
        val root = testDirectory()
        File(root, "screenplay").mkdirs()
        ImageIO.write(BufferedImage(240, 90, BufferedImage.TYPE_INT_RGB).apply { createGraphics().apply { color = java.awt.Color(0x4A90D9); fillRect(0, 0, 240, 90) } }, "png", File(root, "screenplay/hero-1.png"))
        val links = PageLinksState("screenplay/hero.md", root) {}
        links.looks = mapOf("hero-1.png" to MentionLook(Mention.OtherFile("screenplay/hero-1.png"), exists = true))
        val added = mutableListOf<Pair<PageImageSource, String?>>()
        val saved = mutableListOf<String>()
        showing {
            PageEditor(
                name = "hero",
                text = page.replace("## Planned", "![The hero](hero-1.png)\n\n## Planned"),
                onTextChange = { saved += it },
                links = links,
                sprites = listOf("drawable/bat.png"),
                onAddImage = { source, section -> added += source to section },
            )
        }
        waitUntil { onAllNodesWithContentDescription("The hero").fetchSemanticsNodes().isNotEmpty() }
        saveSnapshot("page-editor-image")
        onNodeWithText("Moves").performClick()
        waitForIdle()
        onNode(hasSetTextAction()).performTextInput(" and more")
        onNodeWithText("Image").performClick()
        waitForIdle()
        assertEquals(listOf(true), saved.map { "## Moves and more" in it })
        waitForIdle()
        onNodeWithText("From a file…").assertExists()
        onNodeWithText("A sprite").performClick()
        waitForIdle()
        saveSnapshot("page-editor-image-menu")
        onNodeWithText("bat.png").performClick()
        waitForIdle()
        assertEquals(listOf<Pair<PageImageSource, String?>>(PageImageSource.Sprite("drawable/bat.png") to "Moves and more"), added)
    }

    @Test
    fun aPageThatBreaksTheWritingRulesShowsQuietHintsButADocumentDoesnt() = uiTest(width = 900, height = 420) {
        var isPage by mutableStateOf(true)
        showing { PageEditor(name = "hero", text = "# The hero\n\nThe hero used to fly.\n", onTextChange = {}, showsWritingHints = isPage) }
        onNodeWithText("Writing hints").assertExists()
        onNodeWithText("Line 3, “used to”: Describe the game as it is, in the present tense, without its history.").assertExists()
        saveSnapshot("page-editor-writing-hints")
        isPage = false
        waitForIdle()
        onNodeWithText("Writing hints").assertDoesNotExist()
    }

    @Test
    fun aPlannedItemIsBuiltWithTheAssistantFromItsRightClickMenu() = uiTest(width = 900, height = 520) {
        val prompts = mutableListOf<String>()
        showing {
            CompositionLocalProvider(LocalWritersRoom provides { prompts += it }) {
                PageEditor(name = "hero", text = page.replace("- Wall jump", "- Wall jump\n- Double jump"), onTextChange = {})
            }
        }
        onNodeWithText("Wall jump", substring = true).performMouseInput { rightClick() }
        waitForIdle()
        onNodeWithText("Build this with the assistant").performClick()
        waitForIdle()
        saveSnapshot("page-editor-build-planned")
        onAllNodesWithText("Double jump").onLast().performClick()
        assertEquals(listOf("Let's build this Planned item of the page hero in the writers' room: Double jump"), prompts)
    }
}

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

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.screenplay.MentionIndex
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStore
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.testing.uiTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, EditorMutation::class)
class MentionedInRowTest {

    @Test
    fun theFirstPagesMentioningAFileOpenOnAClickAndTheRestAreInAMenu() = uiTest(width = 700, height = 120) {
        val opened = mutableListOf<String>()
        val pages = listOf("README", "hero", "levels/cave", "levels/forest", "combat", "items")
        val index = MentionIndex(mapOf(".kubriko/actors/Player.json" to pages))
        showing {
            CompositionLocalProvider(LocalMentionedIn provides MentionedIn(File("."), index) { opened += it }) {
                MentionedInRow(path = ".kubriko/actors/Player.json")
                MentionedInRow(path = ".kubriko/actors/Bat.json")
            }
        }
        onNodeWithText("Mentioned in").assertExists()
        onNodeWithText("2 more").assertExists()
        saveSnapshot("mentioned-in")
        onNodeWithText("hero").performClick()
        assertEquals(listOf("hero"), opened)
    }

    @Test
    fun theIndexFollowsThePagesAsTheyChange() = uiTest {
        val root = testDirectory()
        val screenplay = ScreenplayStore(root)
        File(root, "screenplay").mkdirs()
        File(root, "screenplay/hero.md").writeText("[Hero](../.kubriko/actors/Player.json)")
        screenplay.refresh()
        showing {
            val mentionedIn = rememberMentionedIn(root, screenplay.revision, { MentionIndex.of(screenplay.scanNames().associateWith { screenplay.read(it).orEmpty() }) }) {}
            Text(mentionedIn.index.pagesMentioning(".kubriko/actors/Player.json").joinToString().ifEmpty { "None" })
        }
        waitUntil { onAllNodesWithText("hero").fetchSemanticsNodes().isNotEmpty() }
        screenplay.write("README", "[Hero](../.kubriko/actors/Player.json)")
        waitUntil { onAllNodesWithText("README, hero").fetchSemanticsNodes().isNotEmpty() }
    }
}

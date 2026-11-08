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

import com.pandulapeter.kubrikoCreator.screenplay.Mention
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PageLinksStateTest {

    @Test
    fun `each kind of mention is a chip in its accent, and only what can be shown opens`() {
        val opened = mutableListOf<Mention>()
        fun chip(mention: Mention, exists: Boolean = true) = chipOf(MentionLook(mention, exists)) { opened += it }
        assertEquals(Accent.CAST, chip(Mention.ActorType("Player", ".kubriko/actors/Player.json"))?.accent)
        assertEquals(Accent.CREW, chip(Mention.ManagerType("Score", ".kubriko/managers/Score.json"))?.accent)
        assertEquals(Accent.SCENE, chip(Mention.Scene("main", "files/scenes/main.json"))?.accent)
        assertEquals(Accent.SCREENPLAY, chip(Mention.Page("hero", "screenplay/hero.md"))?.accent)
        assertEquals(Accent.AUDIO, chip(Mention.Audio("composeResources/files/hit.wav"))?.accent)
        assertNotNull(chip(Mention.Sprite("composeResources/drawable/ball.png"))?.onClick)
        assertNull(chip(Mention.Sprite("composeResources/drawable/ball.png"), exists = false)?.onClick)
        assertNull(chip(Mention.OtherFile("gradle.properties")))
        val missing = assertNotNull(chip(Mention.Page("villain", "screenplay/villain.md"), exists = false))
        assertEquals(true, missing.isMissing)
        assertNull(missing.onClick)
        assertNotNull(chip(Mention.Document("docs/combat.md"))?.onClick).invoke()
        assertEquals(listOf<Mention>(Mention.Document("docs/combat.md")), opened)
    }

    @Test
    fun `the links of a page are looked up from where it is`() {
        val root = testDirectory()
        File(root, ".kubriko/actors/Player.json").apply { parentFile.mkdirs() }.writeText("{}")
        val looks = mentionLooksOf(root, "screenplay/levels/cave.md", "[Hero](../../.kubriko/actors/Player.json) [Bat](../../.kubriko/actors/Bat.json) [web](https://example.com) [x](#top)")
        assertEquals(
            mapOf(
                "../../.kubriko/actors/Player.json" to MentionLook(Mention.ActorType("Player", ".kubriko/actors/Player.json"), exists = true),
                "../../.kubriko/actors/Bat.json" to MentionLook(Mention.ActorType("Bat", ".kubriko/actors/Bat.json"), exists = false),
            ),
            looks,
        )
    }

    @Test
    fun `addresses wait to be shown, and project files open outside only when that is safe`() {
        val opened = mutableListOf<Mention>()
        val state = PageLinksState("screenplay/README.md", File(".")) { opened += it }
        state.looks = mapOf(
            "README.md" to MentionLook(Mention.Page("README", "screenplay/README.md"), exists = true),
            "../notes.txt" to MentionLook(Mention.OtherFile("notes.txt"), exists = true),
            "../run.command" to MentionLook(Mention.OtherFile("run.command"), exists = true),
            "../gone.txt" to MentionLook(Mention.OtherFile("gone.txt"), exists = false),
        )
        listOf("README.md", "../run.command", "../gone.txt", "#planned").forEach(state::onLinkClick)
        assertEquals(emptyList(), opened)
        assertNull(state.pendingAddress)
        state.onLinkClick("../notes.txt")
        assertEquals(listOf<Mention>(Mention.OtherFile("notes.txt")), opened)
        state.onLinkClick("ftp://example.com")
        assertEquals("ftp://example.com", state.pendingAddress)
        state.onLinkClick("HTTPS://example.com/a")
        assertEquals("HTTPS://example.com/a", state.pendingAddress)
    }
}

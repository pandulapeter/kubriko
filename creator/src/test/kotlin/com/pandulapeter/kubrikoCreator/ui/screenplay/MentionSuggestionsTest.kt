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

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.screenplay.Mention
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MentionSuggestionsTest {
    private fun typed(text: String, cursor: Int = text.length) = TextFieldValue(text, TextRange(cursor))

    @Test
    fun `an at sign starting a word starts a mention`() {
        assertEquals(MentionQuery(4, "pla"), mentionQueryOf(typed("The @pla")))
        assertEquals(MentionQuery(0, ""), mentionQueryOf(typed("@")))
        assertEquals(MentionQuery(1, "x"), mentionQueryOf(typed("(@x)", cursor = 3)))
        assertEquals(MentionQuery(6, "hero"), mentionQueryOf(typed("line\n\n@hero")))
    }

    @Test
    fun `an at sign inside a word, code, a link, or before a space starts none`() {
        listOf("me@home", "`@code", "[@x", "@two words", "@a[b").forEach { assertNull(mentionQueryOf(typed(it)), it) }
        assertNull(mentionQueryOf(TextFieldValue("@pla", TextRange(0, 4))))
        assertNull(mentionQueryOf(typed("no mention")))
    }

    @Test
    fun `choosing writes a link from the page, escaped, with the cursor after it`() {
        val candidate = MentionCandidate("Player", null, Mention.ActorType("Player", ".kubriko/actors/Player.json"))
        val chosen = withMention(typed("The @pl jumps", cursor = 7), MentionQuery(4, "pl"), candidate, "screenplay/levels/cave.md")
        assertEquals("The [Player](../../.kubriko/actors/Player.json) jumps", chosen.text)
        assertEquals(TextRange(47), chosen.selection)
        val odd = MentionCandidate("a]b", null, Mention.Document("docs/a b.md"))
        assertEquals("[a\\]b](../docs/a%20b.md)", withMention(typed("@a"), MentionQuery(0, "a"), odd, "screenplay/README.md").text)
    }

    @Test
    fun `the toolbar starts a mention apart from the word before`() {
        assertEquals(typed("The @"), withMentionStarted(typed("The ")))
        assertEquals(typed("The @"), withMentionStarted(typed("The")))
        assertEquals(typed("@"), withMentionStarted(TextFieldValue("selected", TextRange(0, 8))))
    }

    @Test
    fun `everything the page can mention is offered once, but not the page itself`() {
        fun resource(group: ResourceGroup, path: String) = ProjectResource(group, File(path), path)
        val candidates = mentionCandidatesOf(
            blockTypes = listOf(ManagerTypeDefinition(name = "Score"), ActorTypeDefinition(name = "Ball")),
            resources = mapOf(
                ResourceGroup.SCENES to listOf(resource(ResourceGroup.SCENES, "src/commonMain/composeResources/files/scenes/levels/1-1.json")),
                ResourceGroup.DOCUMENTS to listOf(resource(ResourceGroup.DOCUMENTS, "docs/combat.md")),
                ResourceGroup.SPRITES to listOf(resource(ResourceGroup.SPRITES, "src/commonMain/composeResources/drawable/ball.png")),
            ),
            pageNames = listOf("README", "hero"),
            pagePath = "screenplay/hero.md",
        )
        assertEquals(listOf("Ball", "Score", "levels/1-1", "README", "combat.md", "ball.png"), candidates.map { it.title })
        assertEquals(listOf(null, null, null, null, "docs", "src/commonMain/composeResources/drawable"), candidates.map { it.detail })
        assertEquals(Mention.ManagerType("Score", ".kubriko/managers/Score.json"), candidates[1].mention)
    }

    @Test
    fun `suggestions match the query and stay closed once dismissed`() {
        val candidates = listOf("Ball", "Paddle", "Brick").map { MentionCandidate(it, null, Mention.ActorType(it, ".kubriko/actors/$it.json")) }
        assertEquals(listOf("Ball", "Brick"), mentionSuggestionsOf(typed("@b"), candidates, dismissedAt = null)?.matches?.map { it.title })
        assertNull(mentionSuggestionsOf(typed("@b"), candidates, dismissedAt = 0))
        assertEquals(1, mentionSuggestionsOf(typed("x @b"), candidates, dismissedAt = 0)?.query?.start?.minus(1))
        assertNull(mentionSuggestionsOf(typed("@zzz"), candidates, dismissedAt = null))
    }
}

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.agentclientprotocol.model.ContentBlock
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.git.GitStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class AttentionChipTest {
    private val problem = ConsoleEntry.Problem("Unresolved reference: speed", "Ball.kt:12")

    @Test
    fun theOpenSceneComesBeforeTheSelectedTypeWhichItHides() {
        assertEquals(listOf<AttentionChip>(AttentionChip.Scene("level-1")), AttentionChip.of("level-1", "Ball", false, null))
        assertEquals(listOf<AttentionChip>(AttentionChip.BlockType("Score", true)), AttentionChip.of(null, "Score", true, null))
        assertEquals(emptyList(), AttentionChip.of(null, null, false, null))
    }

    @Test
    fun theLatestProblemIsOfferedAsText() {
        assertEquals(listOf<AttentionChip>(AttentionChip.Problem(problem.message, problem.location)), AttentionChip.of(null, null, false, problem))
        assertEquals(listOf<AttentionChip>(AttentionChip.Problem("The game stopped", null)), AttentionChip.of(null, null, false, ConsoleEntry.SessionFailure("The game stopped")))
    }

    @Test
    fun chipsAreLinksTheAgentFollowsWithCreatorsReadToolsAndAProblemIsText() {
        val scene = AttentionChip.Scene("levels/1-1").toContent() as ContentBlock.ResourceLink
        assertEquals("kubriko-creator://scenes/levels/1-1", scene.uri)
        val type = AttentionChip.BlockType("Ball", isManager = false).toContent() as ContentBlock.ResourceLink
        assertEquals("Actor type Ball", type.name)
        assertEquals(true, type.description?.contains("read_actor_type"))
        assertEquals("The latest problem in the console: Ball.kt:12: Unresolved reference: speed", (AttentionChip.Problem(problem.message, problem.location).toContent() as ContentBlock.Text).text)
    }

    @Test
    fun theGameScreenIsOfferedOnlyWhenItCanBeSentAndIsCapturedWhenSent() = kotlinx.coroutines.test.runTest {
        assertEquals(listOf<AttentionChip>(AttentionChip.GameScreen), AttentionChip.of(null, null, false, null, canSendGameScreen = true))
        val png = byteArrayOf(1, 2, 3)
        val sent = AttentionChip.contentOf(listOf(AttentionChip.Scene("main"), AttentionChip.GameScreen)) { png }
        assertEquals(3, sent.size)
        val image = sent.last() as ContentBlock.Image
        assertEquals("image/png", image.mimeType)
        assertEquals(java.util.Base64.getEncoder().encodeToString(png), image.data)
        assertEquals(1, AttentionChip.contentOf(listOf(AttentionChip.Scene("main"), AttentionChip.GameScreen)) { null }.size)
    }
}

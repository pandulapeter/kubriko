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

import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SavedConversationStoreTest {
    private val directory = createTempDirectory().toFile()
    private val file = File(directory, "conversations.json")
    private val breakout = File("/projects/breakout")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun saved(id: String, updatedAt: Long, agentId: String = "claude-acp", project: File = breakout) =
        SavedConversation(project.absolutePath, agentId, id, "Title $id", updatedAt)

    @Test
    fun conversationsAreKeptPerProjectAndAgentNewestFirst() {
        val store = SavedConversationStore(file)
        store.save(saved("a", 1))
        store.save(saved("b", 2))
        store.save(saved("c", 3, agentId = "antigravity-acp"))
        store.save(saved("d", 4, project = File("/projects/other")))
        assertEquals(listOf("b", "a"), SavedConversationStore(file).of(breakout, "claude-acp").map { it.id })
        assertEquals(listOf("c"), store.of(breakout, "antigravity-acp").map { it.id })
    }

    @Test
    fun savingAgainReplacesItAndOnlyTheNewestStay() {
        val store = SavedConversationStore(file, conversationsPerProject = 2)
        store.save(saved("a", 1))
        store.save(saved("b", 2))
        store.save(saved("a", 3))
        store.save(saved("c", 4))
        assertEquals(listOf("c", "a"), store.of(breakout, "claude-acp").map { it.id })
    }

    @Test
    fun aSummaryStaysUntilATurnNotesAnother() {
        val store = SavedConversationStore(file)
        store.save(saved("a", 1).copy(summary = "Made the Ball faster."))
        store.save(saved("a", 2, agentId = "antigravity-acp"))
        store.save(saved("a", 3))
        assertEquals("Made the Ball faster.", store.of(breakout, "claude-acp").single().summary)
        assertEquals(null, store.of(breakout, "antigravity-acp").single().summary)
        store.save(saved("a", 4).copy(summary = "Added lives."))
        assertEquals("Added lives.", store.of(breakout, "claude-acp").single().summary)
    }

    @Test
    fun removingForgetsOnlyThatConversation() {
        val store = SavedConversationStore(file)
        store.save(saved("a", 1))
        store.save(saved("b", 2))
        store.save(saved("a", 3, agentId = "antigravity-acp"))
        store.remove(breakout, "claude-acp", "a")
        assertEquals(listOf("b"), store.of(breakout, "claude-acp").map { it.id })
        assertEquals(listOf("a"), store.of(breakout, "antigravity-acp").map { it.id })
    }

    @Test
    fun anUnreadableFileMeansNone() {
        file.writeText("nope")
        assertEquals(emptyList(), SavedConversationStore(file).of(breakout, "claude-acp"))
    }

    @Test
    fun anUnreadableFileIsntLost() {
        file.writeText("{ not a list")
        val store = SavedConversationStore(file)
        store.save(saved("a", 1))
        assertEquals("{ not a list", File(directory, "conversations.json.bak").readText())
        assertEquals(listOf("a"), store.of(breakout, "claude-acp").map { it.id })
    }

    @Test
    fun conversationsThatCantBeReadForAMomentArentReplaced() {
        SavedConversationStore(file).apply {
            save(saved("a", 1))
            save(saved("b", 2))
        }
        SavedConversationStore(file, readText = lockedOnce()).save(saved("c", 3))
        assertEquals(listOf("b", "a"), SavedConversationStore(file).of(breakout, "claude-acp").map { it.id })
    }

    /**
     * Reads the file, failing the first time as if another program held it for a moment.
     */
    private fun lockedOnce(): (File) -> String {
        var isLocked = true
        return { file -> if (isLocked) throw IOException("Locked.").also { isLocked = false } else file.readText() }
    }
}

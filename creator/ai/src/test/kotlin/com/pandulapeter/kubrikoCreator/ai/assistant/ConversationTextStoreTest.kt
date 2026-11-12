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
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationTextStoreTest {
    private val directory = createTempDirectory().toFile()
    private val store = ConversationTextStore(File(directory, "conversations"))
    private val messages = listOf(KeptMessage(isFromUser = true, text = "Hi/there\\:*?"))

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun eachConversationIsKeptApartByItsAgentAndIdWhateverTheyHold() {
        store.write("claude-acp", "a/b:c", messages)
        assertEquals(messages, store.read("claude-acp", "a/b:c"))
        assertEquals(emptyList(), store.read("other-acp", "a/b:c"))
        store.delete("claude-acp", "a/b:c")
        assertEquals(emptyList(), store.read("claude-acp", "a/b:c"))
    }

    @Test
    fun aConversationsFileIsTheUsersOnly() {
        store.write("claude-acp", "a", messages)
        val file = File(directory, "conversations").listFiles()!!.single()
        if (File.separatorChar == '/') assertEquals(setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), Files.getPosixFilePermissions(file.toPath()))
        assertTrue(file.name.endsWith(".json"))
    }

    @Test
    fun aFileThatCantBeReadHoldsNothing() {
        store.write("claude-acp", "a", messages)
        File(directory, "conversations").listFiles()!!.single().writeText("not json")
        assertEquals(emptyList(), store.read("claude-acp", "a"))
    }
}

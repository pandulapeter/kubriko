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

import com.pandulapeter.kubrikoCreator.foundation.JsonFileStore
import com.pandulapeter.kubrikoCreator.foundation.valueOrNull
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.security.MessageDigest

/**
 * Keeps each saved conversation's messages ([KeptMessage]) as a file of its own in [directory], readable by the user
 * only, since a conversation may hold anything. The agent holds the transcript itself; this is only for searching
 * conversations and for reading one its agent can't continue. What it read is kept in memory, since only it writes
 * these files and every search reads them all. Every call blocks.
 */
public class ConversationTextStore(
    private val directory: File,
    private val readText: (File) -> String = File::readText,
) {
    private val read = HashMap<String, List<KeptMessage>>()

    /**
     * The messages kept of the conversation [id] held by the agent [agentId], none when nothing was kept or the file
     * can't be read.
     */
    @Synchronized
    internal fun read(agentId: String, id: String): List<KeptMessage> = read.getOrPut(keyOf(agentId, id)) { storeOf(agentId, id).read().valueOrNull().orEmpty() }

    /**
     * Keeps [messages] as the conversation [id] held by the agent [agentId] holds them now.
     */
    @Synchronized
    internal fun write(agentId: String, id: String, messages: List<KeptMessage>) {
        if (storeOf(agentId, id).write(messages)) read[keyOf(agentId, id)] = messages else read.remove(keyOf(agentId, id))
    }

    /**
     * Forgets the messages of the conversation [id] held by the agent [agentId].
     */
    @Synchronized
    internal fun delete(agentId: String, id: String) {
        read.remove(keyOf(agentId, id))
        fileOf(agentId, id).delete()
    }

    private fun keyOf(agentId: String, id: String) = "$agentId\n$id"

    private fun storeOf(agentId: String, id: String) = JsonFileStore(fileOf(agentId, id), ListSerializer(KeptMessage.serializer()), isPrivate = true, readText = readText)

    /**
     * The file of a conversation, named by a hash of the agent's and the conversation's IDs, which may hold anything a
     * file name can't.
     */
    private fun fileOf(agentId: String, id: String): File {
        val hash = MessageDigest.getInstance("SHA-256").digest(keyOf(agentId, id).toByteArray()).joinToString("") { "%02x".format(it) }
        return File(directory, "$hash.json")
    }
}

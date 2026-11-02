/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp

import com.pandulapeter.kubrikoCreator.context.ResultDelta
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationReadsTest {
    private val reads = ConversationReads()
    private val arguments = JsonObject(mapOf("actorType" to JsonPrimitive("Ball")))
    private val result = JsonPrimitive("x".repeat(400))

    private fun read(conversationId: String) = reads.answer(conversationId, "read_actor_type", arguments, result, isFull = false)

    @Test
    fun aRepeatedReadIsADeltaUntilForgotten() {
        assertEquals(result, read("first"))
        assertEquals(JsonPrimitive(ResultDelta.UNCHANGED), read("first"))
        reads.forget("first")
        assertEquals(result, read("first"))
    }

    @Test
    fun forgettingOneConversationKeepsTheOthers() {
        read("first")
        read("second")
        reads.forget("first")
        assertEquals(result, read("first"))
        assertEquals(JsonPrimitive(ResultDelta.UNCHANGED), read("second"))
    }
}

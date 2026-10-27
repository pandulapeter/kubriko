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

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class McpSettingsStoreTest {
    private val directory = createTempDirectory().toFile()
    private val file = File(directory, "settings/mcp.json")
    private var tokenCount = 0
    private val store = McpSettingsStore(file) { "token${++tokenCount}" }

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun theFirstLoadIsOffOnTheDefaultPortWithANewTokenThatIsKept() {
        assertEquals(McpSettings(isEnabled = false, port = McpSettings.DEFAULT_PORT, token = "token1"), store.load())
        assertEquals("token1", store.load().token)
    }

    @Test
    fun savedSettingsAreLoadedAgain() {
        val settings = McpSettings(isEnabled = true, port = 9000, token = "abc")
        store.save(settings)
        assertEquals(settings, McpSettingsStore(file).load())
    }

    @Test
    fun unreadableOrInvalidSettingsStartOver() {
        listOf("{ nope", """{"port":80,"token":"abc"}""", """{"token":""}""").forEachIndexed { index, content ->
            file.parentFile.mkdirs()
            file.writeText(content)
            assertEquals(McpSettings(token = "token${index + 1}"), store.load())
        }
    }

    @Test
    fun newTokensAreRandomAndLong() {
        val tokens = List(5) { McpSettingsStore(file).newToken() }
        assertEquals(5, tokens.toSet().size)
        assertEquals(true, tokens.all { it.length >= 40 })
    }
}

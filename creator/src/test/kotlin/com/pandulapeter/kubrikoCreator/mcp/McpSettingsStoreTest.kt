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

import com.pandulapeter.kubrikoCreator.mcp.clients.AiClient
import org.junit.Assume.assumeTrue
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
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
    fun theTokenIsReadableByItsOwnerOnly() {
        store.load()
        if (file.toPath().fileSystem.supportedFileAttributeViews().contains("posix")) {
            assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file.toPath()))
        }
    }

    @Test
    fun aTokenAnOlderVersionLeftReadableByOthersBecomesReadableByItsOwnerOnly() {
        assumeTrue(file.toPath().fileSystem.supportedFileAttributeViews().contains("posix"))
        file.parentFile.mkdirs()
        file.writeText("""{"isEnabled":true,"port":9000,"token":"abc"}""")
        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-r--r--"))
        assertEquals("abc", store.load().token)
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file.toPath()))
        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-r--r--"))
        store.save(McpSettings(token = "def"))
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file.toPath()))
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
    fun anUnreadableFileIsSetAside() {
        file.parentFile.mkdirs()
        file.writeText("{ nope")
        val isPosix = file.toPath().fileSystem.supportedFileAttributeViews().contains("posix")
        if (isPosix) Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-r--r--"))
        assertEquals("token1", store.load().token)
        val backup = File(file.parentFile, "mcp.json.bak")
        assertEquals("{ nope", backup.readText())
        if (isPosix) {
            assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file.toPath()))
            assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(backup.toPath()))
        }
    }

    @Test
    fun settingsNamingAToolNoLongerListedKeepTheirToken() {
        file.parentFile.mkdirs()
        file.writeText("""{"isEnabled":true,"port":9000,"token":"abc","clientCommands":{"geminiCli":"gemini","claudeCode":"my-claude"}}""")
        val settings = store.load()
        assertEquals("abc", settings.token)
        assertEquals("my-claude", settings.commandOf(AiClient.CLAUDE_CODE))
    }

    @Test
    fun newTokensAreRandomAndLong() {
        val tokens = List(5) { McpSettingsStore(file).newToken() }
        assertEquals(5, tokens.toSet().size)
        assertEquals(true, tokens.all { it.length >= 40 })
    }
}

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp.clients

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class McpConfigFileTest {
    private val directory = createTempDirectory().toFile()
    private val file = File(directory, "config.json")
    private val entry = buildJsonObject { put("url", "http://127.0.0.1:7781/mcp") }
    private val isPosix = directory.toPath().fileSystem.supportedFileAttributeViews().contains("posix")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun theAppsOwnSettingsAndOtherServersAreKeptInTheirOrderAndIndentation() {
        val original = """
            {
                "preferences": {
                    "menuBarEnabled": false,
                    "size": 1.5
                },
                "mcpServers": {
                    "other": {
                        "command": "other-server"
                    }
                },
                "coworkUserFilesPath": "/Users/someone/Claude"
            }
        """.trimIndent() + "\n"
        file.writeText(original)
        McpConfigFile.addServer(file, "kubriko-creator", entry)
        assertEquals(
            """
                {
                    "preferences": {
                        "menuBarEnabled": false,
                        "size": 1.5
                    },
                    "mcpServers": {
                        "other": {
                            "command": "other-server"
                        },
                        "kubriko-creator": {
                            "url": "http://127.0.0.1:7781/mcp"
                        }
                    },
                    "coworkUserFilesPath": "/Users/someone/Claude"
                }
            """.trimIndent() + "\n",
            file.readText(),
        )
        assertEquals(entry, McpConfigFile.server(file, "kubriko-creator"))
        McpConfigFile.removeServer(file, "kubriko-creator")
        assertEquals(original, file.readText())
    }

    @Test
    fun connectingAndDisconnectingLeavesAFileWithoutServersAsItWas() {
        val original = "{\n  \"preferences\": {\n    \"quickEntryShortcut\": \"off\"\n  }\n}"
        file.writeText(original)
        McpConfigFile.addServer(file, "kubriko-creator", entry)
        McpConfigFile.addServer(file, "kubriko-creator", buildJsonObject { put("url", "http://127.0.0.1:7782/mcp") })
        assertEquals(JsonPrimitive("http://127.0.0.1:7782/mcp"), McpConfigFile.server(file, "kubriko-creator")?.get("url"))
        McpConfigFile.removeServer(file, "kubriko-creator")
        assertEquals(original, file.readText())
    }

    @Test
    fun aMissingFileIsCreatedReadableByItsOwnerOnly() {
        val nested = File(directory, "app/mcp.json")
        assertNull(McpConfigFile.server(nested, "kubriko-creator"))
        McpConfigFile.removeServer(nested, "kubriko-creator")
        assertFalse(nested.exists())
        McpConfigFile.addServer(nested, "kubriko-creator", entry)
        assertEquals(entry, McpConfigFile.server(nested, "kubriko-creator"))
        if (isPosix) assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(nested.toPath())))
    }

    @Test
    fun anExistingFileKeepsItsPermissions() {
        if (!isPosix) return
        file.writeText("{}")
        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-r-----"))
        McpConfigFile.addServer(file, "kubriko-creator", entry)
        assertEquals("rw-r-----", PosixFilePermissions.toString(Files.getPosixFilePermissions(file.toPath())))
    }

    @Test
    fun aLinkedFileIsChangedWhereItIsAndStaysLinked() {
        if (!isPosix) return
        val target = File(directory, "dotfiles/config.json").apply { parentFile.mkdirs(); writeText("{}") }
        Files.createSymbolicLink(file.toPath(), target.toPath())
        McpConfigFile.addServer(file, "kubriko-creator", entry)
        assertTrue(Files.isSymbolicLink(file.toPath()))
        assertEquals(entry, McpConfigFile.server(target, "kubriko-creator"))
    }

    @Test
    fun aFileThatIsntAJsonObjectIsNeverWritten() {
        val broken = "{ \"preferences\": "
        file.writeText(broken)
        assertFailsWith<SerializationException> { McpConfigFile.addServer(file, "kubriko-creator", entry) }
        assertFailsWith<SerializationException> { McpConfigFile.removeServer(file, "kubriko-creator") }
        assertEquals(broken, file.readText())
        assertEquals(listOf("config.json"), directory.list()?.toList())
    }
}

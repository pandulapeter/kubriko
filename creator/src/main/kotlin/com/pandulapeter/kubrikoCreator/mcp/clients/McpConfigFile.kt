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

import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.File

/**
 * The `mcpServers` object of an AI app's JSON configuration file, which apps such as Claude Desktop share with their own
 * settings: only the one server entry is ever changed, and everything else is written back with the same values, order,
 * and indentation (numbers and escapes may be spelled differently, as in `1.5` for `1.50`).
 */
internal object McpConfigFile {
    private const val SERVERS = "mcpServers"

    /**
     * The entry of the server called [name] in [file], or null if there is none (or no file).
     *
     * @throws kotlinx.serialization.SerializationException If the file isn't a JSON object.
     */
    fun server(file: File, name: String) = if (file.isFile) server(file.readText(), name) else null

    /**
     * Adds the server called [name] to [file] as [entry], replacing one of that name, and creates the file if needed,
     * readable by its owner only, since entries hold Kubriko Creator's token. A link is followed, and an existing file
     * keeps its permissions.
     *
     * @throws kotlinx.serialization.SerializationException If the file isn't a JSON object, which is then left alone.
     */
    fun addServer(file: File, name: String, entry: JsonObject) = file.writeTextAtomically(withServer(file.takeIf { it.isFile }?.readText(), name, entry), isPrivate = true)

    /**
     * Removes the server called [name] from [file], if both exist.
     *
     * @throws kotlinx.serialization.SerializationException If the file isn't a JSON object, which is then left alone.
     */
    fun removeServer(file: File, name: String) {
        if (!file.isFile) return
        val text = file.readText()
        if (server(text, name) != null) file.writeTextAtomically(withoutServer(text, name), isPrivate = true)
    }

    internal fun server(text: String, name: String) = (parse(text)[SERVERS] as? JsonObject)?.get(name) as? JsonObject

    internal fun withServer(text: String?, name: String, entry: JsonObject): String {
        val root = text?.let(::parse) ?: JsonObject(emptyMap())
        val servers = (root[SERVERS] as? JsonObject).orEmpty()
        return format(JsonObject(root + (SERVERS to JsonObject(servers + (name to entry)))), text)
    }

    /**
     * [text] without the server called [name], and without `mcpServers` if that was the last one, so connecting and
     * disconnecting leaves a file as it was.
     */
    internal fun withoutServer(text: String, name: String): String {
        val root = parse(text)
        val servers = (root[SERVERS] as? JsonObject).orEmpty() - name
        return format(JsonObject(if (servers.isEmpty()) root - SERVERS else root + (SERVERS to JsonObject(servers))), text)
    }

    /**
     * The JSON object in [text], or an empty one for an empty file, as apps leave one they created without settings.
     */
    private fun parse(text: String) = if (text.isBlank()) {
        JsonObject(emptyMap())
    } else {
        Json.parseToJsonElement(text) as? JsonObject ?: throw SerializationException("The file holds JSON, but not an object.")
    }

    /**
     * [root] pretty-printed with the indentation and final line break of [original].
     */
    @OptIn(ExperimentalSerializationApi::class)
    private fun format(root: JsonObject, original: String?): String {
        val indent = original?.lineSequence()?.drop(1)?.firstOrNull { it.isNotBlank() }?.takeWhile { it == ' ' || it == '\t' }?.ifEmpty { null } ?: "  "
        val json = Json {
            prettyPrint = true
            prettyPrintIndent = indent
        }
        return json.encodeToString(JsonObject.serializer(), root) + if (original == null || original.endsWith("\n")) "\n" else ""
    }
}

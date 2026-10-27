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

import com.pandulapeter.kubrikoCreator.mcp.McpSettings
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File

/**
 * An AI app that keeps its MCP servers in a JSON file rather than behind a command, which Kubriko Creator edits (see
 * [McpConfigFile]). The app reads it when it starts.
 *
 * @param id How the settings name it, among the [AiClientSetup.id]s of the connected tools.
 */
internal enum class AiConfigFileClient(
    val id: String,
    val displayName: String,
    val installUrl: String,
) {
    /**
     * Claude Desktop only starts local servers that talk over standard input and output, so `mcp-remote` (run by npx)
     * passes the requests on. An app started from the Finder lacks the shell's PATH, so npx is named by its full path,
     * with its folder (where node is too) put first on the PATH. The token goes into the environment because Claude
     * Desktop on Windows doesn't quote arguments with spaces.
     */
    CLAUDE_DESKTOP("claudeDesktop", "Claude Desktop", "https://claude.ai/download") {
        override val requiredProgram = RequiredProgram("npx", "Node.js", "https://nodejs.org")

        override fun configFile(home: File, operatingSystem: String, appData: String?) = when {
            "mac" in operatingSystem -> File(home, "Library/Application Support/Claude/claude_desktop_config.json")
            "windows" in operatingSystem -> File(appData ?: "$home/AppData/Roaming", "Claude/claude_desktop_config.json")
            else -> File(home, ".config/Claude/claude_desktop_config.json")
        }

        override fun entry(settings: McpSettings, programPath: String?) = buildJsonObject {
            put("command", programPath ?: "npx")
            putJsonArray("args") {
                add("-y")
                add(MCP_REMOTE)
                add(settings.url)
                add("--header")
                add("Authorization:\${$AUTHORIZATION_VARIABLE}")
            }
            putJsonObject("env") {
                put(AUTHORIZATION_VARIABLE, "Bearer ${settings.token}")
                programPath?.let { path -> File(path).parent?.let { put("PATH", "$it:/usr/local/bin:/usr/bin:/bin") } }
            }
        }
    },

    CURSOR("cursor", "Cursor", "https://cursor.com/download") {
        override fun configFile(home: File, operatingSystem: String, appData: String?) = File(home, ".cursor/mcp.json")

        override fun entry(settings: McpSettings, programPath: String?) = buildJsonObject {
            put("url", settings.url)
            putJsonObject("headers") {
                put("Authorization", "Bearer ${settings.token}")
            }
        }
    };

    /**
     * A program the entry runs, which has to be installed for the app to reach Kubriko Creator.
     */
    open val requiredProgram: RequiredProgram? = null

    /**
     * Where the app keeps its servers. The app counts as installed once it has made the folder.
     */
    abstract fun configFile(
        home: File = File(System.getProperty("user.home")),
        operatingSystem: String = System.getProperty("os.name").orEmpty().lowercase(),
        appData: String? = System.getenv("APPDATA"),
    ): File

    /**
     * @param programPath The full path of [requiredProgram], where it was found (never on Windows, whose apps get the
     * user's PATH anyway).
     */
    abstract fun entry(settings: McpSettings, programPath: String?): JsonObject

    /**
     * @param command What runs it.
     * @param name What it's called, for the user.
     */
    data class RequiredProgram(
        val command: String,
        val name: String,
        val installUrl: String,
    )

    private companion object {
        const val MCP_REMOTE = "mcp-remote@0.15"
        const val AUTHORIZATION_VARIABLE = "KUBRIKO_CREATOR_AUTHORIZATION"
    }
}

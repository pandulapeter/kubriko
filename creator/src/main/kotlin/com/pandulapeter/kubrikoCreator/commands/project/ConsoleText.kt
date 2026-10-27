/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.getString

/**
 * A console entry as the AI reads it: what kind of line it is, and its text.
 */
internal suspend fun ConsoleEntry.toJson() = buildJsonObject {
    when (this@toJson) {
        is ConsoleEntry.Status -> {
            put("kind", if (isError) "error" else "status")
            put("text", getString(text, *arguments.toTypedArray()))
        }
        is ConsoleEntry.Problem -> {
            put("kind", "problem")
            put("text", message)
            location?.let { put("location", it) }
        }
        is ConsoleEntry.BuildOutput -> {
            put("kind", "buildOutput")
            put("text", line)
        }
        is ConsoleEntry.Log -> {
            put("kind", if (isImportant) "importantLog" else "log")
            put("text", message)
            source?.let { put("source", it) }
        }
        is ConsoleEntry.SessionFailure -> {
            put("kind", "error")
            put("text", message)
        }
    }
}

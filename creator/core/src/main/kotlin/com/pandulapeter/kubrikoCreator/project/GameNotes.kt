/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import java.io.File

/**
 * The game's design notes (mechanics, naming, and decisions) as a plain file at the project's root, `AGENTS.md`, which
 * coding agents read by themselves; Claude Code reads it through a `CLAUDE.md` that imports it, created beside it when the
 * project has none. Every call blocks.
 */
public object GameNotes {
    public const val FILE_NAME: String = "AGENTS.md"
    private const val CLAUDE_FILE_NAME = "CLAUDE.md"
    private const val CLAUDE_IMPORT = "@$FILE_NAME\n"

    public fun read(root: File): String? = File(root, FILE_NAME).takeIf { it.isFile }?.readText()

    /**
     * Replaces the notes with [text], or deletes them for null.
     */
    @EditorMutation
    public fun write(root: File, text: String?) {
        val file = File(root, FILE_NAME)
        if (text == null) {
            file.delete()
            return
        }
        file.writeTextAtomically(text)
        val claude = File(root, CLAUDE_FILE_NAME)
        if (!claude.exists()) claude.writeTextAtomically(CLAUDE_IMPORT)
    }
}

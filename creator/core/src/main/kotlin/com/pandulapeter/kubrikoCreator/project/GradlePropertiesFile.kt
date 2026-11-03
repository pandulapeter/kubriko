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
 * The `gradle.properties` file of a project, where its settings live ([GameIdentity], [GameSettings]).
 */
public object GradlePropertiesFile {
    public const val NAME: String = "gradle.properties"

    /**
     * The text of the file in [rootDirectory] with Unix line breaks, or null if there is none.
     */
    public fun read(rootDirectory: File): String? = File(rootDirectory, NAME).takeIf { it.isFile }?.readText()?.withUnixLineBreaks()

    /**
     * Writes the file in [rootDirectory] as [change] makes its text (with Unix line breaks, empty if there is no file), in
     * one write that keeps its line breaks.
     */
    @EditorMutation
    public fun update(rootDirectory: File, change: (String) -> String) {
        val file = File(rootDirectory, NAME)
        val raw = if (file.isFile) file.readText() else ""
        val changed = change(raw.withUnixLineBreaks())
        if (changed != raw.withUnixLineBreaks()) file.writeTextAtomically(changed.withLineBreaksOf(raw))
    }
}

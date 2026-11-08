/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.pandulapeter.kubrikoCreator.screenplay.MovedFiles
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection

/**
 * Tells the page or document the page editor shows apart from the one before it, but not from itself once Kubriko
 * Creator moved it ([MovedFiles]), so that the editor keeps what is being typed across the move.
 */
internal class MarkdownIdentity(
    private val movedFiles: MovedFiles,
) {
    private var path: String? = null
    private var identity = Any()

    /**
     * The identity of what [selection] shows: the one before, if [selection] is where that was moved to.
     */
    fun of(selection: WorkspaceSelection): Any {
        val newPath = markdownPathOf(selection)
        val oldPath = path
        if (newPath != oldPath) {
            if (oldPath == null || newPath == null || movedFiles.whereIs(oldPath) != newPath) identity = Any()
            path = newPath
        }
        return identity
    }
}

/**
 * The [MarkdownIdentity] of what [selection] shows.
 */
@Composable
internal fun rememberMarkdownIdentity(selection: WorkspaceSelection, movedFiles: MovedFiles): Any = remember(movedFiles) { MarkdownIdentity(movedFiles) }.of(selection)

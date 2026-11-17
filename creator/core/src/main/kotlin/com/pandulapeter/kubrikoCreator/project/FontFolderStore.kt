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

import java.io.File

/**
 * The sidebar's folders of fonts ([VirtualFolderStore], in `.kubriko/fonts`), since Compose resources make accessors only
 * of the fonts right in `font`.
 */
public class FontFolderStore(
    rootDirectory: File,
    ownWrites: OwnWrites,
) : VirtualFolderStore<FontFolderStore.State>(rootDirectory, ownWrites, DIRECTORY, State()) {
    /**
     * @param folders Every folder, as paths (`titles/pixel`), empty ones too.
     * @param folderOf The folder of each font in one, by the font's name.
     */
    public data class State(
        override val folders: Set<String> = emptySet(),
        override val folderOf: Map<String, String> = emptyMap(),
    ) : VirtualFolders

    override fun stateOf(folders: Set<String>, folderOf: Map<String, String>, filesByName: Map<String, List<File>>): State = State(folders, folderOf)

    public companion object {
        public const val DIRECTORY: String = ".kubriko/fonts"
    }
}

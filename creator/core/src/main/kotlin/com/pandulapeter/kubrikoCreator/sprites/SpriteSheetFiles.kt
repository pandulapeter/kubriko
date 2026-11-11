/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sprites

import java.io.File

/**
 * Reads the sheets kept in the sprites' files ([SpriteFolderStore]), parsing only the files whose length or modification
 * time changed since it last read them, since the code is regenerated from them after every batch of changes. Calls
 * block, and are safe from any thread.
 */
public class SpriteSheetFiles {
    private val known = HashMap<File, Known>()

    private class Known(val length: Long, val lastModified: Long, val sheet: SpriteSheet?)

    /**
     * How each sprite of the project at [rootDirectory] is cut into frames, by its name, as [SpriteFolderStore.State.sheets]
     * says.
     */
    public fun read(rootDirectory: File): Map<String, SpriteSheet> = sheetsOf(SpriteFolderStore.filesByNameIn(rootDirectory))

    /**
     * The sheets of [filesByName] (each sprite's files, by its name, the one counting first), leaving out the ones that
     * can't be read or played.
     */
    @Synchronized
    internal fun sheetsOf(filesByName: Map<String, List<File>>): Map<String, SpriteSheet> {
        val firsts = filesByName.mapValues { (_, files) -> files.first() }
        known.keys.retainAll(firsts.values.toSet())
        return firsts.mapNotNull { (name, file) ->
            val length = file.length()
            val lastModified = file.lastModified()
            val cached = known[file]?.takeIf { it.length == length && it.lastModified == lastModified }
            val sheet = cached?.sheet ?: SpriteFolderStore.sheetOf(file).also { known[file] = Known(length, lastModified, it) }
            sheet?.let { name to it }
        }.toMap()
    }
}

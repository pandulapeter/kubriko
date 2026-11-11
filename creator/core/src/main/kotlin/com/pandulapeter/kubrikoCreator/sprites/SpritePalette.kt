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

import com.pandulapeter.kubrikoCreator.actors.DefinitionPaths
import com.pandulapeter.kubrikoCreator.actors.HexColorSerializer
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.JsonFileStore
import com.pandulapeter.kubrikoCreator.foundation.valueOrNull
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException

/**
 * The colors the project keeps for drawing its sprites, in `.kubriko/palette.json`, which the sprite editor offers and the
 * AI reads: the same few colors across sprites make a game look of one piece. A project without one has none. Calls that
 * change it block, so callers run them off the main thread; [colors] is read anywhere.
 */
public class SpritePalette(
    private val rootDirectory: File,
    private val ownWrites: OwnWrites,
) {
    private val file get() = File(rootDirectory, FILE)
    private val store get() = JsonFileStore(file, Content.serializer())
    private val _colors = MutableStateFlow(emptyList<Long>())

    /**
     * The colors, each `0xAARRGGBB`, in the order they were added.
     */
    public val colors: StateFlow<List<Long>> = _colors.asStateFlow()

    /**
     * Reads the palette again. Blocks.
     */
    public fun refresh() {
        _colors.value = store.read().valueOrNull()?.colors.orEmpty()
    }

    /**
     * Makes [newColors] the palette; an empty one deletes the file. Throws [IOException] when it can't be written.
     */
    @EditorMutation
    public fun set(newColors: List<Long>) {
        if (newColors.isEmpty()) {
            if (!file.delete() && file.exists()) throw IOException("${file.path} couldn't be deleted.")
        } else if (!store.write(Content(newColors))) {
            throw IOException("${file.path} couldn't be written.")
        }
        ownWrites.record(file)
        refresh()
    }

    @Serializable
    private class Content(
        @SerialName("colors") val colors: List<@Serializable(with = HexColorSerializer::class) Long> = emptyList(),
    )

    public companion object {
        public const val FILE: String = "${DefinitionPaths.CREATOR_DIRECTORY}/palette.json"
    }
}

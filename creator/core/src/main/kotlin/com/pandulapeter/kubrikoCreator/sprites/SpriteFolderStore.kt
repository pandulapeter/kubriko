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

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.project.VirtualFolderStore
import com.pandulapeter.kubrikoCreator.project.VirtualFolders
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.io.IOException

/**
 * The sidebar's folders of sprites ([VirtualFolderStore], in `.kubriko/sprites`), since Compose resources make accessors
 * only of the images right in `drawable`. A sprite's file also holds how it is cut into frames ([setSheet]).
 */
public class SpriteFolderStore(
    rootDirectory: File,
    ownWrites: OwnWrites,
) : VirtualFolderStore<SpriteFolderStore.State>(rootDirectory, ownWrites, DIRECTORY, State()) {
    /**
     * @param folders Every folder, as paths (`enemies/flying`), empty ones too.
     * @param folderOf The folder of each sprite in one, by the sprite's name.
     * @param sheets How each sprite cut into frames is cut, by the sprite's name; a sheet that can't be played
     *   ([SpriteSheet.problem]) is left out, so the image shows whole.
     */
    public data class State(
        override val folders: Set<String> = emptySet(),
        override val folderOf: Map<String, String> = emptyMap(),
        val sheets: Map<String, SpriteSheet> = emptyMap(),
    ) : VirtualFolders

    private val sheetFiles = SpriteSheetFiles()

    override fun stateOf(folders: Set<String>, folderOf: Map<String, String>, filesByName: Map<String, List<File>>): State =
        State(folders = folders, folderOf = folderOf, sheets = sheetFiles.sheetsOf(filesByName))

    /**
     * Cuts the sprite called [name] into frames as [sheet] says, or shows it whole for null, keeping the rest of what its
     * file holds. A sprite left with nothing to keep and in no folder has no file. Throws [java.io.IOException] when the
     * file can't be written, changing nothing.
     */
    @EditorMutation
    public fun setSheet(name: String, sheet: SpriteSheet?) {
        val files = filesOf(name)
        val content = files.firstOrNull()?.let(::textOf)?.let(::contentOf) ?: JsonObject(emptyMap())
        val text = textOf(JsonObject(if (sheet == null) content - SHEET else content + (SHEET to json.encodeToJsonElement(SpriteSheet.serializer(), sheet))))
        val folder = state.value.folderOf[name].orEmpty()
        val target = files.firstOrNull() ?: entryOf(name, folder)
        if (text == DEFAULT_TEXT && folder.isEmpty()) {
            files.forEach { file ->
                file.delete()
                ownWrites.record(file)
            }
        } else {
            files.ifEmpty { listOf(target) }.forEach { file ->
                file.parentFile.mkdirs()
                file.writeTextAtomically(text)
                ownWrites.record(file)
            }
        }
        refresh()
    }

    public companion object {
        public const val DIRECTORY: String = ".kubriko/sprites"
        public const val DEFAULT_TEXT: String = VirtualFolderStore.DEFAULT_TEXT
        private const val SHEET = "sheet"
        private val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }

        /**
         * The files of the sprites of the project at [rootDirectory], by each sprite's name, in order of their paths.
         * Blocks.
         */
        internal fun filesByNameIn(rootDirectory: File): Map<String, List<File>> = VirtualFolderStore.filesByNameIn(rootDirectory, DIRECTORY)

        /**
         * What [file] holds, or null when it can't be read.
         */
        internal fun textOf(file: File): String? = try {
            file.readText()
        } catch (_: IOException) {
            null
        }

        /**
         * The sheet a sprite's file holding [text] keeps, or null if it keeps none, or one that can't be read or played.
         */
        internal fun sheetOf(text: String?): SpriteSheet? = text?.let(::contentOf)?.get(SHEET)?.let { element ->
            try {
                json.decodeFromJsonElement(SpriteSheet.serializer(), element).takeIf { it.problem == null }
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        /**
         * What a sprite's file holding [text] keeps, or null when it isn't a JSON object.
         */
        private fun contentOf(text: String) = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (_: SerializationException) {
            null
        }

        private fun textOf(content: JsonObject) = if (content.isEmpty()) DEFAULT_TEXT else json.encodeToString(JsonObject.serializer(), content) + "\n"
    }
}

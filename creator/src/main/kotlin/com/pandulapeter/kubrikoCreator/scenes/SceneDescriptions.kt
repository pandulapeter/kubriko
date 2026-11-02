/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * The one-line descriptions of a project's Scenes, which say what each is for (the first level, the menu), kept in
 * `.kubriko/scenes.json` by the Scene's name, since a Scene file holds nothing but its Actors. A file that can't be read
 * counts as holding none.
 */
internal class SceneDescriptions(
    rootDirectory: File,
    private val ownWrites: OwnWrites,
) {
    private val file = File(rootDirectory, FILE)
    private val _current = MutableStateFlow(read())

    /**
     * The descriptions as Kubriko Creator last read or wrote them, for the user interface.
     */
    val current = _current.asStateFlow()

    /**
     * The descriptions as the file has them now.
     */
    fun all() = read().also { _current.value = it }

    private fun read(): Map<String, String> = try {
        if (file.isFile) json.decodeFromString(Contents.serializer(), file.readText()).descriptions else emptyMap()
    } catch (_: SerializationException) {
        emptyMap()
    } catch (_: IllegalArgumentException) {
        emptyMap()
    } catch (_: IOException) {
        emptyMap()
    }

    fun of(scene: String) = all()[scene]

    /**
     * Gives the Scene [scene] [description], or takes its description away if it is null or blank.
     */
    @EditorMutation
    fun set(scene: String, description: String?) = change { descriptions ->
        if (description.isNullOrBlank()) descriptions - scene else descriptions + (scene to description)
    }

    @EditorMutation
    fun move(oldName: String, newName: String) = change { descriptions ->
        descriptions[oldName]?.let { descriptions - oldName + (newName to it) } ?: descriptions
    }

    private fun change(change: (Map<String, String>) -> Map<String, String>) {
        val previous = read()
        val next = change(previous).toSortedMap()
        if (next == previous) return
        file.parentFile.mkdirs()
        file.writeTextAtomically(json.encodeToString(Contents.serializer(), Contents(next)) + "\n")
        ownWrites.record(file)
        _current.value = next
    }

    @Serializable
    private data class Contents(
        @SerialName("descriptions") val descriptions: Map<String, String> = emptyMap(),
    )

    private companion object {
        const val FILE = ".kubriko/scenes.json"
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    }
}

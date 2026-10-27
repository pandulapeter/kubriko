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

import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import kotlinx.serialization.SerializationException
import java.io.File

/**
 * The Scene files of a project, by name (the file name without `.json`). Every call blocks, so callers that write run
 * them off the main thread.
 *
 * @param sceneResources The Scene files the last scan of the project found. Scenes created since then are found in the
 *   folder new Scenes go to.
 */
internal class SceneStore(
    private val rootDirectory: File,
    private val sceneResources: () -> List<ProjectResource>,
) {
    /**
     * Where new Scenes go: next to the existing ones, or where the project templates keep them.
     */
    private val sceneDirectory get() = sceneResources().firstOrNull()?.file?.parentFile ?: File(rootDirectory, DEFAULT_SCENE_DIRECTORY)

    fun names() = (sceneResources().map { it.file } + sceneDirectory.listFiles { file -> file.extension == EXTENSION }.orEmpty())
        .filter { it.isFile }
        .map { it.nameWithoutExtension }
        .distinct()
        .sorted()

    fun exists(name: String) = file(name) != null

    fun read(name: String) = file(name)?.readText()

    @EditorMutation
    fun write(name: String, content: String) {
        val file = file(name) ?: File(sceneDirectory, "$name.$EXTENSION")
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    @EditorMutation
    fun delete(name: String) {
        file(name)?.delete()
    }

    /**
     * Points the Actors of every Scene that are of the [oldTypeId] type to [newTypeId].
     *
     * @return The names of the Scenes that changed.
     */
    @EditorMutation
    fun renameTypeId(oldTypeId: String, newTypeId: String) = names().filter { name ->
        val content = read(name) ?: return@filter false
        val actors = try {
            SceneDocument.parse(content)
        } catch (_: SerializationException) {
            return@filter false
        }
        if (actors.none { it.typeId == oldTypeId }) return@filter false
        write(name, SceneDocument.format(actors.map { if (it.typeId == oldTypeId) it.copy(typeId = newTypeId) else it }))
        true
    }

    private fun file(name: String) = sceneResources().firstOrNull { it.file.nameWithoutExtension == name && it.file.isFile }?.file
        ?: File(sceneDirectory, "$name.$EXTENSION").takeIf { it.isFile }

    companion object {
        const val DEFAULT_SCENE_DIRECTORY = "src/commonMain/composeResources/files/scenes"
        private const val EXTENSION = "json"
    }
}

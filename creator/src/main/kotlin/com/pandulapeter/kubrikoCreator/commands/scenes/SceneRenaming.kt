/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.scenes

import com.pandulapeter.kubrikoCreator.actors.withSceneRenamed
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.ProjectTree
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Moves the Scene [oldName] to [newName], with every goToScene Block, and says in the console which hand-written Kotlin
 * files still name it.
 *
 * @return Whether the file was moved; if it wasn't, nothing changed.
 */
@EditorMutation
internal suspend fun CommandContext.renameScene(oldName: String, newName: String): Boolean {
    if (!withContext(ioDispatcher) { scenes.move(oldName, newName) }) return false
    blockTypes?.let { blockTypes ->
        blockTypes.types.value.forEach { definition ->
            val renamed = definition.withSceneRenamed(oldName, newName)
            if (renamed != definition) blockTypes.put(renamed)
        }
    }
    withContext(ioDispatcher) { handWrittenFilesNaming(project.rootDirectory, oldName) }.forEach { path ->
        report(ConsoleEntry.Status(CommandProblem.SCENE_NAMED_IN_CODE, isError = true, arguments = listOf(path, oldName, newName)))
    }
    return true
}

/**
 * Why the Scene [name] couldn't be moved.
 */
internal fun sceneNotMoved(name: String) = Rejection(CommandProblem.SCENE_NOT_MOVED, name)

/**
 * The Kotlin files outside generated code that name the Scene [name] in a string, like `Game.kt` names the first one.
 */
private fun handWrittenFilesNaming(rootDirectory: File, name: String) = ProjectTree.walk(rootDirectory, skipped = creatorDirectories)
    .filter { it.isFile && it.extension == "kt" && "\"$name\"" in it.readText() }
    .map { it.relativeTo(rootDirectory).invariantSeparatorsPath }
    .sorted()
    .toList()

private val creatorDirectories = setOf(".kubriko", "generated")

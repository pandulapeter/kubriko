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
import com.pandulapeter.kubrikoCreator.context.HandWrittenKotlin
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.GameSettings
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Moves the Scene [oldName] to [newName], with every goToScene Block and the game's first Scene, and says in the console
 * which hand-written Kotlin files still name it.
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
    try {
        withContext(ioDispatcher) { followFirstScene(project.rootDirectory, oldName, newName) }
    } catch (exception: IOException) {
        report(ConsoleEntry.Status(CommandProblem.FIRST_SCENE_NOT_FOLLOWED, isError = true, arguments = listOf(oldName, exception.message ?: exception.toString())))
    }
    withContext(ioDispatcher) { HandWrittenKotlin.read(project.rootDirectory) }.filterValues { "\"$oldName\"" in it }.keys.sorted().forEach { path ->
        report(ConsoleEntry.Status(CommandProblem.SCENE_NAMED_IN_CODE, isError = true, arguments = listOf(path, oldName, newName)))
    }
    return true
}

@EditorMutation
private fun followFirstScene(rootDirectory: File, oldName: String, newName: String) {
    val settings = firstSceneSettings(rootDirectory)?.takeIf { it.firstScene == oldName } ?: return
    GameSettings.write(rootDirectory, settings.copy(firstScene = newName), listOf(GameSettings.FIRST_SCENE))
}

/**
 * The game's settings, if its build reads them and its first Scene is one it can read.
 */
internal fun firstSceneSettings(rootDirectory: File) = GameSettings.read(rootDirectory)?.takeIf { GameSettings.FIRST_SCENE !in it.invalidKeys }?.settings

/**
 * Why the Scene [name] couldn't be moved.
 */
internal fun sceneNotMoved(name: String) = Rejection(CommandProblem.SCENE_NOT_MOVED, name)

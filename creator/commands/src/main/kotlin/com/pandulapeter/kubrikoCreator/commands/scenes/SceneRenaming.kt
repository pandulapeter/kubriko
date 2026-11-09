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

import com.pandulapeter.kubrikoCreator.actors.withScenesRenamed
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
internal suspend fun CommandContext.renameScene(oldName: String, newName: String): Boolean = renameScenes(mapOf(oldName to newName)) == null

/**
 * Moves each Scene of [renamed] (each old name with its new one), then the goToScene Blocks and the game's first Scene
 * naming them, and says in the console which hand-written Kotlin files still name them; the types, the settings, and the
 * Kotlin are each gone through once, however many Scenes move. It stops at the first Scene that can't be moved, moving
 * back the ones moved before it, so that nothing changes.
 *
 * @return The Scene that couldn't be moved, or null if every one was.
 */
@EditorMutation
internal suspend fun CommandContext.renameScenes(renamed: Map<String, String>): String? {
    if (renamed.isEmpty()) return null
    val notMoved = withContext(ioDispatcher) {
        val moved = ArrayList<String>()
        for ((oldName, newName) in renamed) {
            if (!scenes.move(oldName, newName)) {
                moved.asReversed().forEach { scenes.move(renamed.getValue(it), it) }
                return@withContext oldName
            }
            moved += oldName
        }
        null
    }
    if (notMoved != null) return notMoved
    blockTypes?.let { blockTypes ->
        blockTypes.types.value.forEach { definition ->
            val withScenesRenamed = definition.withScenesRenamed(renamed)
            if (withScenesRenamed != definition) blockTypes.put(withScenesRenamed)
        }
    }
    followFirstScene(renamed)
    reportScenesNamedInCode(renamed)
    return null
}

/**
 * Says in the console which hand-written Kotlin files still name a Scene of [renamed], looking through each file once for
 * all of them.
 */
private suspend fun CommandContext.reportScenesNamedInCode(renamed: Map<String, String>) {
    val namesByPath = withContext(ioDispatcher) {
        HandWrittenKotlin.read(project.rootDirectory).mapValues { (_, text) -> quotedTexts(text).filter { it in renamed } }
    }
    renamed.forEach { (oldName, newName) ->
        namesByPath.filterValues { oldName in it }.keys.sorted().forEach { path ->
            report(ConsoleEntry.Status(CommandProblem.SCENE_NAMED_IN_CODE, isError = true, arguments = listOf(path, oldName, newName)))
        }
    }
}

/**
 * Every piece of [text] between two of its consecutive quotes: each string literal's content, among others, so that a
 * name is in it exactly when [text] holds the name in quotes.
 */
internal fun quotedTexts(text: String): Set<String> = text.split('"').drop(1).dropLast(1).toSet()

/**
 * Makes the game start from the Scene's new name if it starts from one of [renamed], saying so in the console if it can't.
 */
@EditorMutation
private suspend fun CommandContext.followFirstScene(renamed: Map<String, String>) {
    var firstScene: String? = null
    try {
        withContext(ioDispatcher) {
            val settings = firstSceneSettings(project.rootDirectory) ?: return@withContext
            val newName = renamed[settings.firstScene] ?: return@withContext
            firstScene = settings.firstScene
            GameSettings.write(project.rootDirectory, settings.copy(firstScene = newName), listOf(GameSettings.FIRST_SCENE))
        }
    } catch (exception: IOException) {
        report(ConsoleEntry.Status(CommandProblem.FIRST_SCENE_NOT_FOLLOWED, isError = true, arguments = listOf(firstScene ?: renamed.keys.first(), exception.message ?: exception.toString())))
    }
}

/**
 * The game's settings, if its build reads them and its first Scene is one it can read.
 */
internal fun firstSceneSettings(rootDirectory: File) = GameSettings.read(rootDirectory)?.takeIf { GameSettings.FIRST_SCENE !in it.invalidKeys }?.settings

/**
 * Why the Scene [name] couldn't be moved.
 */
internal fun sceneNotMoved(name: String) = Rejection(CommandProblem.SCENE_NOT_MOVED, name)

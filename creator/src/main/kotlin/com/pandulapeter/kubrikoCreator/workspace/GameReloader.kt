/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.session.ClassShape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_game_changes_not_applied
import kubriko.creator.generated.resources.status_game_reload_failed
import kubriko.creator.generated.resources.status_game_reloaded
import kubriko.creator.generated.resources.status_game_restarting
import java.io.File
import java.io.IOException

/**
 * Brings a game started with Compose Hot Reload up to date with every change made while it plays: the project is built,
 * and the changed classes are reloaded into the running game when only their code changed, or the game is started again
 * when a class changed in a way a reload can't apply ([ClassShape]).
 *
 * Changes made while one is applied are applied together after it.
 *
 * @param buildClasses Builds the project, returning the folders of its own classes, or null if the build failed.
 * @param isGameRunning Waits until the game that plays has started, returning whether one plays.
 * @param reload Runs the reload task with the given name, returning whether it succeeded.
 * @param restartGame Starts the game again, as it was started last time.
 */
internal class GameReloader(
    scope: CoroutineScope,
    private val buildClasses: suspend () -> List<File>?,
    private val readShapes: suspend (List<File>) -> Map<String, ClassShape>,
    private val isGameRunning: suspend () -> Boolean,
    private val reload: suspend (taskName: String) -> Boolean,
    private val restartGame: () -> Unit,
    private val console: ConsoleLog,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    /**
     * A game that can be reloaded, and the shapes of the classes it runs.
     */
    private class Game(
        val reloadTaskName: String,
        var shapes: Map<String, ClassShape>,
    )

    @Volatile
    private var game: Game? = null
    private val changes = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            for (change in changes) {
                try {
                    applyChanges()
                } catch (exception: IOException) {
                    console.append(ConsoleEntry.Status(Res.string.status_game_changes_not_applied, isError = true, arguments = listOf(exception.message ?: exception.toString())))
                }
            }
        }
    }

    /**
     * Records the game about to start: one that [reloadTaskName] reloads, running classes of [shapes], or, when
     * [reloadTaskName] is null, one that changes don't reach.
     */
    fun onGameStarting(reloadTaskName: String?, shapes: Map<String, ClassShape>) {
        game = reloadTaskName?.let { Game(it, shapes) }
    }

    /**
     * Applies the project's changes to the game that plays, if it can be reloaded.
     */
    fun onCodeChanged() {
        if (game != null) changes.trySend(Unit)
    }

    private suspend fun applyChanges() {
        val game = game ?: return
        if (!isGameRunning()) return
        val folders = buildClasses() ?: return
        val built = readShapes(folders)
        if (this.game !== game) return
        val changedClasses = ClassShape.changesNeedingRestart(game.shapes, built)
        if (changedClasses.isNotEmpty()) {
            console.append(ConsoleEntry.Status(Res.string.status_game_restarting, arguments = listOf(typeNamesOf(changedClasses).joinToString())))
            restartGame()
            return
        }
        val startTime = currentTimeMillis()
        if (reload(game.reloadTaskName)) {
            game.shapes += built
            console.append(ConsoleEntry.Status(Res.string.status_game_reloaded, arguments = listOf(((currentTimeMillis() - startTime) / 100 / 10f).toString())))
        } else if (this.game === game) {
            console.append(ConsoleEntry.Status(Res.string.status_game_reload_failed, isError = true))
            restartGame()
        }
    }

    /**
     * The top-level types [classNames] belong to, by their simple names, as the user knows them.
     */
    private fun typeNamesOf(classNames: List<String>) = classNames.map { it.substringAfterLast('/').substringBefore('$') }.distinct()
}

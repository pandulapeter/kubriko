/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.git.GitController
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.workspace.BlockTypesController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_changes_not_written
import kubriko.creator.generated.resources.error_command_generated_code_not_written

/**
 * What commands read and change: the parts of the open project.
 *
 * @param blockTypes The project's block-built types, or null if the project hasn't opted in to them.
 * @param audioFiles The sounds and music of the project, or null if they aren't known, so they aren't checked.
 * @param spriteFiles The images Actor types can be drawn as, or null if they aren't known, so they aren't checked.
 * @param onScenesChanged Tells the rest of the editor which Scene files a command changed.
 * @param onSceneRenamed Tells the rest of the editor that a command moved a Scene to another name (and so file).
 * @param report Adds an entry to the console, for what the user should know after a command.
 * @param onProjectUpgraded Tells the rest of the editor that the project's template files were brought up to date.
 * @param rejectChanges Why no change may be made to the project at all right now (it was saved by a newer Kubriko
 *   Creator), or null if changes may be made.
 */
internal class CommandContext(
    val project: CreatorProject,
    val console: StateFlow<List<ConsoleEntry>>,
    val blockTypes: BlockTypesController?,
    val git: GitController,
    val scenes: SceneStore,
    val runner: ProjectRunner,
    val ioDispatcher: CoroutineDispatcher,
    val audioFiles: () -> AudioFiles? = { null },
    val spriteFiles: () -> SpriteFiles? = { null },
    val onScenesChanged: (names: Collection<String>) -> Unit,
    val onSceneRenamed: (oldName: String, newName: String) -> Unit = { _, _ -> },
    val report: (ConsoleEntry) -> Unit = {},
    val onProjectUpgraded: () -> Unit = {},
    val rejectChanges: () -> Rejection? = { null },
) {

    /**
     * Returns once every change made so far is on disk, or writing them failed, for what reads the files (git, and builds);
     * whether they all are on disk.
     */
    suspend fun awaitWrites() = blockTypes?.awaitWrites() ?: true

    /**
     * Why a command that reads the files is refused while [awaitWrites] says they aren't all on disk.
     */
    fun unsavedWrite(): Rejection {
        val unsaved = checkNotNull(blockTypes).unsavedWrites()
        return when (val subjects = unsaved.subjects) {
            null -> Rejection(Res.string.error_command_generated_code_not_written, unsaved.reason)
            else -> Rejection(Res.string.error_command_changes_not_written, subjects, unsaved.reason)
        }
    }

    /**
     * Reads the types again after a change rewrote their files, returning once they are in memory, so that the next
     * command doesn't write the old ones back.
     */
    suspend fun reloadTypes() {
        blockTypes?.reload()
    }
}

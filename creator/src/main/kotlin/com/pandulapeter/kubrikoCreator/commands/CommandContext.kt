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
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * What commands read and change: the parts of the open project.
 *
 * @param blockTypes The project's block-built types, or null if the project hasn't opted in to them.
 * @param audioFiles The sounds and music of the project, or null if they aren't known, so they aren't checked.
 * @param spriteFiles The images Actor types can be drawn as, or null if they aren't known, so they aren't checked.
 * @param report Adds an entry to the console, for what the user should know after a command.
 * @param onProjectUpgraded Tells the rest of the editor that the project's template files were brought up to date.
 * @param projectUpgrade What the project's template files need, as last checked in the background, so that validating
 *   reads no files.
 * @param rejectChanges Why no change may be made to the project at all right now (it was saved by a newer Kubriko
 *   Creator), or null if changes may be made.
 */
internal class CommandContext(
    val project: CreatorProject,
    val console: StateFlow<List<ConsoleEntry>>,
    val blockTypes: BlockTypes?,
    val git: GitController,
    val scenes: SceneStore,
    val runner: ProjectRunner,
    val ioDispatcher: CoroutineDispatcher,
    val audioFiles: () -> AudioFiles? = { null },
    val spriteFiles: () -> SpriteFiles? = { null },
    val report: (ConsoleEntry) -> Unit = {},
    val onProjectUpgraded: () -> Unit = {},
    val projectUpgrade: () -> TemplateUpgrade = { TemplateUpgrade.UpToDate },
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
            null -> Rejection(CommandProblem.GENERATED_CODE_NOT_WRITTEN, unsaved.reason)
            else -> Rejection(CommandProblem.CHANGES_NOT_WRITTEN, subjects, unsaved.reason)
        }
    }

    /**
     * Reads the types again after a change rewrote their files, returning once they are in memory, so that the next
     * command doesn't write the old ones back.
     */
    suspend fun reloadTypes() {
        blockTypes?.reload()
    }

    /**
     * A hash of the content of each type and Scene in [targets], by its target without Event or Block indices, for telling
     * whether it changed since. A deleted type or Scene hashes as 0, so one that is still deleted matches. A Scene that
     * exists but can't be read right now (the running game holds it open on Windows) gets null, and so does every other
     * kind of target: a null hash isn't compared.
     */
    suspend fun contentHashesOf(targets: Collection<CommandTarget>): Map<CommandTarget, Int?> {
        val hashes = LinkedHashMap<CommandTarget, Int?>()
        val sceneNames = ArrayList<String>()
        targets.forEach { target ->
            when (target) {
                is CommandTarget.BlockType -> hashes[CommandTarget.BlockType(target.name)] = blockTypes?.let { it[target.name].hashCode() }
                is CommandTarget.Scene -> sceneNames += target.name
                else -> Unit
            }
        }
        if (sceneNames.isNotEmpty()) {
            withContext(ioDispatcher) {
                sceneNames.forEach { name -> hashes[CommandTarget.Scene(name)] = if (scenes.fileOf(name).isFile) scenes.read(name)?.hashCode() else 0 }
            }
        }
        return hashes
    }
}

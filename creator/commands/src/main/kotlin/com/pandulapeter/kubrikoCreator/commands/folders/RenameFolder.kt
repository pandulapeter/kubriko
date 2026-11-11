/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.folders

import com.pandulapeter.kubrikoCreator.actors.DefinitionPaths
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.CompositeCommand
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.resources.resourceFilesOnDisk
import com.pandulapeter.kubrikoCreator.commands.screenplay.projectPathOf
import com.pandulapeter.kubrikoCreator.commands.screenplay.validatePageName
import com.pandulapeter.kubrikoCreator.commands.screenplay.withMentionsFollowed
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.PathMove
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@OptIn(EditorMutation::class)
public object RenameFolder : WriteCommand<RenameFolder.Input> {
    override val name: String = "rename_folder"
    override val description: String = "Renames a folder of Actor types, Manager types, Scenes, pages, sounds, or music, keeping everything in it where it is in it. A Scene's " +
        "or audio file's name is its path, so it changes too, and what uses it follows (but hand-written Kotlin), as do the screenplay's mentions of everything in it."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateFolder(input.group, input.path) ?: when {
        input.path.isEmpty() -> Rejection(CommandProblem.FOLDER_TOP)
        else -> FolderPaths.validateName(input.newName)?.let(::Rejection) ?: context.validateNewFolder(input.group, renamed(input)) ?: validateMovedPages(context, input)
    }

    /**
     * Why a page or a folder in the folder can't be named as it would be after the rename (its name, with its folders,
     * grows past the limit), or null if every one can.
     */
    private fun validateMovedPages(context: CommandContext, input: Input): Rejection? = if (input.group == FolderGroup.PAGES) {
        (context.itemsOf(FolderGroup.PAGES).filter { (_, folder) -> FolderPaths.isWithin(folder, input.path) }.map { (page, _) -> page } +
            context.foldersOf(FolderGroup.PAGES).filter { it != input.path && FolderPaths.isWithin(it, input.path) })
            .firstNotNullOfOrNull { path -> validatePageName(renamed(input) + path.removePrefix(input.path)) }
    } else {
        null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val newPath = renamed(input)
        val scenesBefore = if (input.group == FolderGroup.SCENES) sceneFilesWithin(context, input.path) else emptyMap()
        val (audioMoves, audioRenames) = if (input.group.resourceKind != null) {
            withContext(context.ioDispatcher) {
                context.audioFolderMoves(input.path, newPath) to audioFilesMovedWith(context.resourceFilesOnDisk().map { it.path }.distinct(), input.path, newPath)
            }
        } else {
            emptyList<PathMove>() to emptyMap()
        }
        context.moveFolder(input.group, input.path, newPath)?.let { rejection -> return Applied(inverse = null, failure = rejection) }
        val renamingBack = Invocation(RenameFolder, Input(input.group, newPath, FolderPaths.nameOf(input.path)))
        val inverse = if (input.group.resourceKind == null) {
            renamingBack
        } else {
            // Restoring, since the resource scan that knows the moved files may come after the undo.
            val movingBack = renamingBack.copy(input = renamingBack.input.copy(movesOnly = true), isRestoring = true)
            val restoring = listOf(movingBack) + if (input.movesOnly) emptyList() else context.followAudioFiles(audioRenames)
            restoring.singleOrNull() ?: Invocation(CompositeCommand, CompositeCommand.Input(restoring))
        }
        val moves = when (val kind = input.group.kind) {
            null -> if (input.group.resourceKind != null) {
                audioMoves
            } else if (input.group == FolderGroup.PAGES) {
                listOf(PathMove("${PageNaming.FOLDER}/${input.path}", "${PageNaming.FOLDER}/$newPath"))
            } else {
                withContext(context.ioDispatcher) {
                    scenesBefore.map { (scene, file) -> PathMove(file, context.projectPathOf(context.scenes.fileOf(newPath + scene.removePrefix(input.path)))) }
                }
            }
            else -> listOf(PathMove(DefinitionPaths.folderPath(kind, input.path), DefinitionPaths.folderPath(kind, newPath)))
        }
        return context.withMentionsFollowed(moves, Applied(inverse = inverse))
    }

    /**
     * The file of every Scene in [folder], by the Scene's name: a folder of Scenes may be in more than one module.
     */
    private suspend fun sceneFilesWithin(context: CommandContext, folder: String) = withContext(context.ioDispatcher) {
        context.scenes.scanNames().filter { FolderPaths.isWithin(FolderPaths.parentOf(it), folder) }.associateWith { context.projectPathOf(context.scenes.fileOf(it)) }
    }

    private fun renamed(input: Input) = FolderPaths.child(FolderPaths.parentOf(input.path), input.newName)

    /**
     * @param movesOnly Whether an audio folder's files only move, leaving what uses them: how undo and redo move them,
     *   while the steps beside it put back exactly what the rename changed.
     */
    @Serializable
    public data class Input(
        @Description("What the folder holds.")
        @SerialName("group") val group: FolderGroup,
        @Description("The folder's path: folder names separated by slashes, like enemies/flying (for audio, from composeResources/files).")
        @SerialName("path") val path: String,
        @Description("Its new name (only the last part of the path changes).")
        @SerialName("newName") val newName: String,
        @Transient val movesOnly: Boolean = false,
    )
}

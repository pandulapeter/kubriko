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

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.screenplay.projectPathOf
import com.pandulapeter.kubrikoCreator.commands.screenplay.withMentionsFollowed
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.scenes.SceneNaming
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.screenplay.PathMove
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object RenameScene : WriteCommand<RenameScene.Input> {
    override val name: String = "rename_scene"
    override val description: String = "Renames a Scene, or moves it into another folder: a Scene's name is its path, like levels/1-1. Every goToScene Block " +
        "and mention in the screenplay follows; hand-written Kotlin that names the Scene doesn't, and the console says where it is."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Scene(input.newName)

    override fun validate(context: CommandContext, input: Input): Rejection? = when {
        !context.scenes.exists(input.scene) -> Rejection(CommandProblem.SCENE_MISSING, input.scene)
        else -> SceneNaming.validate(input.newName)?.let(::Rejection) ?: validateFreeName(context, input)
    }

    private fun validateFreeName(context: CommandContext, input: Input): Rejection? = when {
        SceneNaming.reservedFolderOf(input.newName) != null -> Rejection(CommandProblem.SCENE_FOLDER_RESERVED, SceneNaming.reservedFolderOf(input.newName).orEmpty())
        context.scenes.names().any { it != input.scene && it.equals(input.newName, ignoreCase = true) } -> Rejection(CommandProblem.SCENE_NAME_TAKEN)
        else -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val isTaken = withContext(context.ioDispatcher) { context.scenes.scanNames().any { it != input.scene && it.equals(input.newName, ignoreCase = true) } }
        if (isTaken) return Applied.failed(Rejection(CommandProblem.SCENE_NAME_TAKEN))
        val from = withContext(context.ioDispatcher) { context.projectPathOf(context.scenes.fileOf(input.scene)) }
        if (!context.renameScene(input.scene, input.newName)) return Applied(inverse = null, failure = sceneNotMoved(input.scene))
        val to = withContext(context.ioDispatcher) { context.projectPathOf(context.scenes.fileOf(input.newName)) }
        return context.withMentionsFollowed(listOf(PathMove(from, to)), Applied(inverse = Invocation(RenameScene, Input(input.newName, input.scene))))
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
        @Description("Its new name, which is also where it goes: folder names and a name separated by slashes, like levels/forest/1-1.")
        @SerialName("newName") val newName: String,
    )
}

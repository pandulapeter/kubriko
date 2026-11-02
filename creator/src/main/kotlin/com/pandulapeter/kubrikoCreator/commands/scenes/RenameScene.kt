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
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.scenes.SceneNaming
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_rename_scene
import kubriko.creator.generated.resources.error_command_scene_missing
import kubriko.creator.generated.resources.error_scene_folder_reserved
import kubriko.creator.generated.resources.error_scene_name_invalid
import kubriko.creator.generated.resources.error_scene_name_taken

@OptIn(EditorMutation::class)
internal object RenameScene : WriteCommand<RenameScene.Input> {
    override val name = "rename_scene"
    override val description = "Renames a Scene, or moves it into another folder: a Scene's name is its path, like levels/1-1. Every goToScene Block " +
        "follows; hand-written Kotlin that names the Scene doesn't, and the console says where it is."
    override val title = Res.string.command_rename_scene
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.newName)

    override fun validate(context: CommandContext, input: Input): Rejection? = when {
        !context.scenes.exists(input.scene) -> Rejection(Res.string.error_command_scene_missing, input.scene)
        !SceneNaming.isValid(input.newName) -> Rejection(Res.string.error_scene_name_invalid)
        SceneNaming.reservedFolderOf(input.newName) != null -> Rejection(Res.string.error_scene_folder_reserved, SceneNaming.reservedFolderOf(input.newName).orEmpty())
        context.scenes.names().any { it != input.scene && it.equals(input.newName, ignoreCase = true) } -> Rejection(Res.string.error_scene_name_taken)
        else -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        if (!context.renameScene(input.scene, input.newName)) return Applied(inverse = null, failure = sceneNotMoved(input.scene))
        return Applied(inverse = Invocation(RenameScene, Input(input.newName, input.scene)))
    }

    @Serializable
    data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
        @Description("Its new name, which is also where it goes: folder names and a name separated by slashes, like levels/forest/1-1.")
        @SerialName("newName") val newName: String,
    )
}

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
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_create_scene
import kubriko.creator.generated.resources.error_command_scene_content_invalid
import kubriko.creator.generated.resources.error_scene_name_invalid
import kubriko.creator.generated.resources.error_scene_name_taken

@OptIn(EditorMutation::class)
internal object CreateScene : WriteCommand<CreateScene.Input> {
    override val name = "create_scene"
    override val description = "Creates a Scene file, empty unless its content is given. A game only loads the Scenes its code names."
    override val title = Res.string.command_create_scene
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = when {
        !namePattern.matches(input.scene) -> Rejection(Res.string.error_scene_name_invalid)
        context.scenes.names().any { it.equals(input.scene, ignoreCase = true) } -> Rejection(Res.string.error_scene_name_taken)
        !SceneDocument.isValid(input.content) -> Rejection(Res.string.error_command_scene_content_invalid)
        else -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        withContext(context.ioDispatcher) { context.scenes.write(input.scene, input.content) }
        context.onScenesChanged(listOf(input.scene))
        return Applied(inverse = Invocation(DeleteScene, DeleteScene.Input(input.scene)))
    }

    private val namePattern = Regex("[A-Za-z0-9][A-Za-z0-9_-]*")

    @Serializable
    data class Input(
        @Description("The name of the new Scene: letters, numbers, dashes, and underscores. It becomes the file name, with .json added.")
        @SerialName("scene") val scene: String,
        @Description("The content of the Scene file, as read_scene returns it in its raw form. An empty Scene if left out.")
        @SerialName("content") val content: String = "[]",
    )
}

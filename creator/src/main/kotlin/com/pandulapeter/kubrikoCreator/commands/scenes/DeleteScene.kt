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
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_delete_scene
import kubriko.creator.generated.resources.error_command_scene_missing

@OptIn(EditorMutation::class)
internal object DeleteScene : WriteCommand<DeleteScene.Input> {
    override val name = "delete_scene"
    override val description = "Deletes a Scene file."
    override val title = Res.string.command_delete_scene
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = if (context.scenes.exists(input.scene)) null else Rejection(Res.string.error_command_scene_missing, input.scene)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val content = withContext(context.ioDispatcher) { context.scenes.read(input.scene).orEmpty().also { context.scenes.delete(input.scene) } }
        context.onScenesChanged(listOf(input.scene))
        return Applied(inverse = Invocation(CreateScene, CreateScene.Input(input.scene, content)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its path in the Scenes folder without .json, like level-2 or levels/1-1.")
        @SerialName("scene") val scene: String,
    )
}

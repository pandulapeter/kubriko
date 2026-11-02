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
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_scene_missing

@OptIn(EditorMutation::class)
internal object DeleteScene : WriteCommand<DeleteScene.Input> {
    override val name = "delete_scene"
    override val description = "Deletes a Scene file."
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = if (context.scenes.exists(input.scene)) null else Rejection(Res.string.error_command_scene_missing, input.scene)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val (content, description) = withContext(context.ioDispatcher) {
            (context.scenes.read(input.scene).orEmpty() to context.scenes.descriptions.of(input.scene)).also { context.scenes.delete(input.scene) }
        }
        return Applied(inverse = Invocation(CreateScene, CreateScene.Input(input.scene, content, description)))
    }

    @Serializable
    data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
    )
}

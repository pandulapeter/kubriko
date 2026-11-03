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
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object DeleteScene : WriteCommand<DeleteScene.Input> {
    override val name: String = "delete_scene"
    override val description: String = "Deletes a Scene file, unless the game starts with it."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = if (context.scenes.exists(input.scene)) null else Rejection(CommandProblem.SCENE_MISSING, input.scene)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val isFirst = withContext(context.ioDispatcher) { firstSceneSettings(context.project.rootDirectory)?.firstScene == input.scene }
        if (isFirst) return Applied.failed(Rejection(CommandProblem.SCENE_IS_FIRST, input.scene))
        val (content, description) = withContext(context.ioDispatcher) {
            (context.scenes.read(input.scene).orEmpty() to context.scenes.descriptions.of(input.scene)).also { context.scenes.delete(input.scene) }
        }
        return Applied(inverse = Invocation(CreateScene, CreateScene.Input(input.scene, content, description)))
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
    )
}

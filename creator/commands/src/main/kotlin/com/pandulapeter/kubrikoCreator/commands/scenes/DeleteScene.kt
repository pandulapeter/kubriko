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
import com.pandulapeter.kubrikoCreator.commands.DeletingCommand
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.deleteWithUsages
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@OptIn(EditorMutation::class)
public object DeleteScene : DeletingCommand<DeleteScene.Input> {
    override val name: String = "delete_scene"
    override val description: String = "Deletes a Scene file, unless the game starts with it. " + Descriptions.DELETE_CLEANUP
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun subject(input: Input): RelationSubject = RelationSubject.Scene(input.scene)

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = if (context.scenes.exists(input.scene)) null else Rejection(CommandProblem.SCENE_MISSING, input.scene)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val isFirst = !input.isUndoingCreation && withContext(context.ioDispatcher) { firstSceneSettings(context.project.rootDirectory)?.firstScene == input.scene }
        if (isFirst) return Applied.failed(Rejection(CommandProblem.SCENE_IS_FIRST, input.scene))
        if (!input.keepsUsages && !input.isUndoingCreation) {
            return context.deleteWithUsages(subject(input), Invocation(DeleteScene, input.copy(keepsUsages = true)))
        }
        val (content, description) = withContext(context.ioDispatcher) {
            (context.scenes.read(input.scene).orEmpty() to context.scenes.descriptions.of(input.scene)).also { context.scenes.delete(input.scene) }
        }
        return Applied(inverse = Invocation(CreateScene, CreateScene.Input(input.scene, content, description)))
    }

    /**
     * @param isUndoingCreation Whether this undoes the Scene's creation, which goes ahead even if the game starts with it,
     *   since the game started with a missing Scene before then too. Nothing could refer to it before then either, so
     *   it leaves what does now as it is.
     * @param keepsUsages Whether it deletes only the Scene itself, leaving what refers to it: the last part of the delete,
     *   after [deleteWithUsages] cleaned that up.
     */
    @Serializable
    public data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
        @Transient val isUndoingCreation: Boolean = false,
        @Transient val keepsUsages: Boolean = false,
    )
}

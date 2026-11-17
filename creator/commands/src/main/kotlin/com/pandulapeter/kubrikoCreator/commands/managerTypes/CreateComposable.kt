/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.managerTypes

import com.pandulapeter.kubrikoCreator.actors.codegen.ManagerComposables
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.CompositeCommand
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.RemoveCreatedFile
import com.pandulapeter.kubrikoCreator.commands.ReplaceFiles
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireManagerType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateManagerType
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException

/**
 * Makes the file of a new hand-written Composable for a Manager type to show, and shows it. The file is the user's from
 * then on, so undoing removes it only while it still holds what it was made with ([RemoveCreatedFile]).
 */
@OptIn(EditorMutation::class)
public object CreateComposable : WriteCommand<CreateComposable.Input> {
    override val name: String = "create_composable"
    override val description: String = "Makes a Kotlin file in the ui package with a new @Composable a Manager type shows on top of the game, " +
        "fun <name>(<manager>Manager: <ManagerType>, windowInsets: WindowInsets), for its UI in Compose; its Properties are Compose state. The file is the project's own code."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(input.managerType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateManagerType(input.managerType) ?: when {
        context.requireBlockTypes.codeLocation == null -> Rejection(CommandProblem.NO_ACTOR_TYPES)
        !ManagerComposables.isValidFunctionName(nameOf(input), input.managerType) -> Rejection(CommandProblem.COMPOSABLE_NAME_INVALID, nameOf(input))
        else -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireManagerType(input.managerType)
        val location = checkNotNull(context.requireBlockTypes.codeLocation)
        val name = nameOf(input)
        val path = ManagerComposables.pathOf(location, name)
        val bytes = ManagerComposables.textOf(location, definition.name, name).toByteArray()
        if (withContext(context.ioDispatcher) { File(context.project.rootDirectory, path).exists() }) return Applied.failed(Rejection(CommandProblem.COMPOSABLE_FILE_TAKEN, path))
        val file = try {
            withContext(context.ioDispatcher) { ReplaceFiles.writeNew(context, path, bytes) }
        } catch (exception: IOException) {
            return Applied.failed(Rejection(CommandProblem.COMPOSABLE_NOT_WRITTEN, path, exception.message ?: exception.toString()))
        }
        try {
            context.requireBlockTypes.put(definition.copy(composable = ManagerComposables.qualifiedNameOf(location, name)))
        } catch (exception: Exception) {
            withContext(context.ioDispatcher) { runCatching { ReplaceFiles.delete(context, file) }.exceptionOrNull()?.let(exception::addSuppressed) }
            throw exception
        }
        val inverse = CompositeCommand.Input(
            listOf(
                Invocation(SetComposable, SetComposable.Input(definition.name, definition.composable.orEmpty())),
                Invocation(RemoveCreatedFile, RemoveCreatedFile.Input(path, bytes)),
            ),
        )
        return Applied(inverse = Invocation(CompositeCommand, inverse))
    }

    private fun nameOf(input: Input) = input.name?.ifEmpty { null } ?: ManagerComposables.defaultNameOf(input.managerType)

    @Serializable
    public data class Input(
        @Description(Descriptions.MANAGER_TYPE_NAME)
        @SerialName("managerType") val managerType: String,
        @Description("The function's name, which its file is named after too. Left out, the Manager type's name with Ui after it.")
        @SerialName("name") val name: String? = null,
    )
}

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

import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.CreateActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DiscardChangesMadeByHand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetAppearance
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventKey
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventOtherActorType
import com.pandulapeter.kubrikoCreator.commands.git.Commit
import com.pandulapeter.kubrikoCreator.commands.git.DiscardChanges
import com.pandulapeter.kubrikoCreator.commands.git.StartVersionControl

/**
 * Every command of the editor. The AI tool list is generated from it, so a command reaches the AI as soon as it is
 * registered, and a test fails if a command exists that isn't.
 */
internal class CommandRegistry(
    val commands: List<EditorCommand<*>>,
) {
    private val commandsByName = commands.associateBy { it.name }

    init {
        require(commandsByName.size == commands.size) { "Command names must be unique." }
    }

    operator fun get(name: String) = commandsByName[name]

    val tools by lazy {
        commands.map { command ->
            ToolDefinition(
                name = command.name,
                description = command.description,
                inputSchema = JsonSchema.of(command.inputSerializer.descriptor),
                isReadOnly = command is ReadCommand,
            )
        }
    }

    companion object {
        val editor = CommandRegistry(
            commands = listOf(
                CreateActorType,
                RenameActorType,
                DeleteActorType,
                SetAppearance,
                AddEvent,
                RemoveEvent,
                MoveEvent,
                SetEventKey,
                SetEventOtherActorType,
                AddBlock,
                EditBlock,
                MoveBlock,
                RemoveBlock,
                DiscardChangesMadeByHand,
                StartVersionControl,
                Commit,
                DiscardChanges,
            ),
        )
    }
}

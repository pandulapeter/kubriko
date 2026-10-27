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

import kotlinx.serialization.KSerializer
import org.jetbrains.compose.resources.StringResource

/**
 * One operation of the editor: a [ReadCommand], a [WriteCommand], or an [ActionCommand]. Every command is registered in [CommandRegistry], which the user interface, undo and
 * redo, and the AI tool list all go through, so the AI can do exactly what the user can.
 *
 * @param I The command's input, a serializable class whose properties carry a [Description] each. Its JSON schema is
 *   the input schema of the command's AI tool.
 */
internal sealed interface EditorCommand<I> {

    /**
     * The stable identifier of the command, and the name of its AI tool: lowercase words joined by underscores.
     */
    val name: String

    /**
     * What the command does, for both people and models.
     */
    val description: String

    val inputSerializer: KSerializer<I>

    /**
     * How the user interface names the command: in the titles of undo and redo, and while the AI runs it.
     */
    val title: StringResource

    /**
     * What the command with [input] reads or changes, so that the editor can show it while the AI runs it. Asked both
     * before and after the command runs, so it can name what the command created.
     */
    fun target(context: CommandContext, input: I): CommandTarget? = null
}

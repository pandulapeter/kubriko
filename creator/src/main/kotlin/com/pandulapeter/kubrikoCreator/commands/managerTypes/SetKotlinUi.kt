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

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireManagerType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateManagerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_set_kotlin_ui

@OptIn(EditorMutation::class)
internal object SetKotlinUi : WriteCommand<SetKotlinUi.Input> {
    override val name = "set_kotlin_ui"
    override val description = "Replaces the Kotlin UI of a block-built Manager type: Compose code inserted verbatim into its generated Composable, " +
        "drawn on top of the game after its texts, for UI the texts can't express (menus, bars, layouts). In scope: windowInsets (the WindowInsets " +
        "of the game's view), the Manager type's Properties (Compose state, so the UI follows them; numbers are Floats), isInSceneEditor, every other " +
        "Manager type as a field named after it with a lowercase first letter (Lives as lives, unless a Property has that name, then livesManager), " +
        "actorManager, viewportManager, and stateManager (isRunning tells whether the game is paused). Read their StateFlows with collectAsState() " +
        "(imported from androidx.compose.runtime), or the UI won't follow them. Import lines at the start of the code become " +
        "the generated file's imports; nothing else is imported for it. Renaming a type or a Property renames it in the code too, outside strings and comments. Empty code removes it."
    override val title = Res.string.command_set_kotlin_ui
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.managerType)

    override fun validate(context: CommandContext, input: Input) = context.validateManagerType(input.managerType)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireManagerType(input.managerType)
        context.requireBlockTypes.put(definition.copy(kotlinUi = input.code.takeIf { it.isNotBlank() }))
        return Applied(inverse = Invocation(SetKotlinUi, input.copy(code = definition.kotlinUi.orEmpty())))
    }

    override fun mergeKey(input: Input) = input.managerType to name

    @Serializable
    data class Input(
        @Description("The name of the Manager type.")
        @SerialName("managerType") val managerType: String,
        @Description("The Kotlin code: optional import lines, then the statements of the Composable, such as Column { BasicText(\"Lives: \${lives.count.toInt()}\") }.")
        @SerialName("code") val code: String,
    )
}

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.screenplay

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.PathMove
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object MovePage : WriteCommand<MovePage.Input> {
    override val name: String = "move_page"
    override val description: String = "Renames a page of the screenplay, or moves it into another folder. Every link to it, and its own links, follow."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Page(if (context.screenplay.existingName(input.newPage) == input.newPage) input.newPage else input.page)

    override fun targets(context: CommandContext, input: Input): Set<CommandTarget> = setOf(CommandTarget.Page(input.page), CommandTarget.Page(input.newPage))

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validatePage(input.page)
        ?: if (input.newPage == input.page) Rejection(CommandProblem.PAGE_TAKEN, input.newPage) else context.validateNewPageName(input.newPage, renamed = input.page)

    /**
     * Moves a page back to its name, which the user may have given it outside Kubriko Creator.
     */
    override fun validateRestoring(context: CommandContext, input: Input): Rejection? = context.validatePage(input.page)
        ?: context.screenplay.existingName(input.newPage)?.takeIf { !it.equals(input.page, ignoreCase = true) }?.let { Rejection(CommandProblem.PAGE_TAKEN, input.newPage) }

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        if (context.screenplay.scanExistingName(input.newPage)?.let { it != input.page } == true) {
            return@withContext Applied.failed(Rejection(CommandProblem.PAGE_TAKEN, input.newPage))
        }
        if (!context.screenplay.move(input.page, input.newPage)) return@withContext Applied.failed(Rejection(CommandProblem.PAGE_NOT_MOVED, input.page))
        context.withMentionsFollowed(
            moves = listOf(PathMove(PageNaming.pathOf(input.page), PageNaming.pathOf(input.newPage))),
            applied = Applied(inverse = Invocation(MovePage, Input(input.newPage, input.page))),
        )
    }

    @Serializable
    public data class Input(
        @Description(PAGE_NAME_DESCRIPTION)
        @SerialName("page") val page: String,
        @Description("Its new name, with the folders it goes into, like levels/cave.")
        @SerialName("newPage") val newPage: String,
    )
}

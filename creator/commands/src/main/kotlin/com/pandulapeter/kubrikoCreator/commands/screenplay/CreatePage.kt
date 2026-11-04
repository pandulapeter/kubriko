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
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object CreatePage : WriteCommand<CreatePage.Input> {
    override val name: String = "create_page"
    override val description: String = "Creates a page of the screenplay, the game's design as Markdown in screenplay/: a # title, a one-sentence summary, " +
        "sections under ## headings, and an optional ## Planned list. A game starts as one page, README.md; give a topic a page of its own " +
        "only when the page has grown too long to read in one go, never one page per Actor type, Manager type, or Scene. Describe what " +
        "the player meets and link to the types, rather than restating their Properties or Events."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Page(input.page)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateNewPageName(input.page) ?: validatePageText(input.text)

    /**
     * Brings back a deleted page whatever its name and length, which the user may have given it outside Kubriko Creator.
     */
    override fun validateRestoring(context: CommandContext, input: Input): Rejection? =
        context.screenplay.existingName(input.page)?.let { Rejection(CommandProblem.PAGE_TAKEN, input.page) }

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        if (context.screenplay.scanExistingName(input.page) != null) return@withContext Applied.failed(Rejection(CommandProblem.PAGE_TAKEN, input.page))
        context.screenplay.write(input.page, input.text)
        Applied(inverse = Invocation(DeletePage, DeletePage.Input(input.page)))
    }

    @Serializable
    public data class Input(
        @Description(PAGE_NAME_DESCRIPTION)
        @SerialName("page") val page: String,
        @Description("The page's Markdown.")
        @SerialName("text") val text: String,
    )
}

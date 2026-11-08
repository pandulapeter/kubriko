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
import com.pandulapeter.kubrikoCreator.screenplay.PageText
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object EditPage : WriteCommand<EditPage.Input> {
    override val name: String = "edit_page"
    override val description: String = "Replaces a page of the screenplay, or only one of its sections."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Page(input.page)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validatePage(input.page) ?: validatePageText(input.text)

    override fun validateByAi(context: CommandContext, input: Input): Rejection? = context.validatePageStyle(PageNaming.pathOf(input.page), input.text, isWholePage = input.section == null)

    /**
     * Brings back what the page held, however long it was.
     */
    override fun validateRestoring(context: CommandContext, input: Input): Rejection? = context.validatePage(input.page)

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        val previous = context.screenplay.read(input.page) ?: return@withContext Applied.failed(Rejection(CommandProblem.PAGE_MISSING, input.page))
        val text = input.section?.let { section ->
            PageText.withSectionReplaced(previous, section, input.text) ?: return@withContext Applied.failed(Rejection(CommandProblem.PAGE_SECTION_MISSING, input.page, section))
        } ?: input.text
        if (input.section != null) validatePageText(text)?.let { return@withContext Applied.failed(it) }
        context.screenplay.write(input.page, text)
        Applied(inverse = Invocation(EditPage, Input(input.page, previous)))
    }

    /**
     * Typing into one page, or one of its sections, is one step to undo.
     */
    override fun mergeKey(input: Input): Any? = Triple(input.page, input.section, name)

    /**
     * @param section The heading of the section [text] replaces, or null for the whole page.
     */
    @Serializable
    public data class Input(
        @Description(PAGE_NAME_DESCRIPTION)
        @SerialName("page") val page: String,
        @Description("The Markdown: the whole page, or the section with its own heading line when section is given.")
        @SerialName("text") val text: String,
        @Description("The heading of the one section to replace (without the # marks), or none for the whole page.")
        @SerialName("section") val section: String? = null,
    )
}

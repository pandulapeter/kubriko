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
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.DeletingCommand
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.deleteWithUsages
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public object DeletePage : DeletingCommand<DeletePage.Input> {
    override val name: String = "delete_page"
    override val description: String = "Deletes a page of the screenplay."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Page(input.page)

    override fun subject(input: Input): RelationSubject = RelationSubject.Page(input.page)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validatePage(input.page)

    override suspend fun apply(context: CommandContext, input: Input): Applied = context.deleteWithUsages(subject(input), Invocation(RemovePage, input))

    @Serializable
    public data class Input(
        @Description(PAGE_NAME_DESCRIPTION)
        @SerialName("page") val page: String,
        @Description("Whether to delete the page's own images too (next to it and named after it) that no other page or document shows.")
        @SerialName("deleteImages") val deleteImages: Boolean = false,
    )
}

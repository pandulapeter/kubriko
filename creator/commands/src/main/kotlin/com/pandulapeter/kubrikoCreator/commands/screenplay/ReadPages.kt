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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

public object ReadPages : ReadCommand<ReadPages.Input> {
    override val name: String = "read_pages"
    override val description: String = "Reads pages of the screenplay, the game's design in its own words, as Markdown: the ones named, or every page."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome = withContext(context.ioDispatcher) {
        val names = input.pages.ifEmpty { context.screenplay.scanNames() }
        val texts = names.associateWith { context.screenplay.read(it) }
        texts.entries.firstOrNull { it.value == null }?.let { return@withContext CommandOutcome.Rejected(Rejection(CommandProblem.PAGE_MISSING, it.key)) }
        CommandOutcome.Success(buildJsonObject { texts.forEach { (page, text) -> put(page, text) } })
    }

    @Serializable
    public data class Input(
        @Description("The names of the pages to read, or none for every page.")
        @SerialName("pages") val pages: List<String> = emptyList(),
    )
}

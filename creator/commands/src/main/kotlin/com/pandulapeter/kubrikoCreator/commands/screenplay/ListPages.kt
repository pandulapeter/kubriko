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
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.screenplay.PageText
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

public object ListPages : ReadCommand<NoInput> {
    override val name: String = "list_pages"
    override val description: String = "Lists the pages of the screenplay, the game's design, each with its title and one-sentence summary."
    override val inputSerializer: KSerializer<NoInput> = NoInput.serializer()

    override suspend fun read(context: CommandContext, input: NoInput): CommandOutcome = CommandOutcome.Success(
        withContext(context.ioDispatcher) {
            buildJsonArray {
                context.screenplay.scanNames().forEach { page ->
                    val text = context.screenplay.read(page).orEmpty()
                    add(
                        buildJsonObject {
                            put("page", page)
                            PageText.titleOf(text)?.let { put("title", it) }
                            PageText.summaryOf(text)?.let { put("summary", it) }
                        },
                    )
                }
            }
        },
    )
}

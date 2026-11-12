/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.context.searchProject
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

public object SearchProject : ReadCommand<SearchProject.Input> {
    override val name: String = "search_project"
    override val description: String = "Finds text in the project: names (types, Properties, Scenes, pages, documents, sprites, sounds, music; letters in order match), " +
        "and lines of descriptions, Log and Set text Blocks, Kotlin Blocks and Kotlin UI, screen texts, pages, and documents containing it. Hits are " +
        "place (kind): text, places written Type#event.block or a path, with :line for code and Markdown."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = null

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        if (input.query.isBlank()) return CommandOutcome.Rejected(Rejection(CommandProblem.SEARCH_QUERY_BLANK))
        val results = context.searchProject(input.query, isSearchingNames = input.includeNames, limit = HIT_LIMIT)
        return CommandOutcome.Success(
            buildJsonObject {
                putJsonArray("hits") { results.hits.forEach { add(it.toString()) } }
                if (results.total > results.hits.size) put("more", results.total - results.hits.size)
            },
        )
    }

    /**
     * How many hits the AI reads at most, enough to act on, few enough to stay cheap; a narrower query finds the rest.
     */
    private const val HIT_LIMIT = 40

    @Serializable
    public data class Input(
        @Description("What to find, ignoring case.")
        @SerialName("query") val query: String = "",
        @Description("Whether to find names too, not only texts.")
        @SerialName("includeNames") val includeNames: Boolean = true,
    )
}

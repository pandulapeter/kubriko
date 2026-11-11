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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

public object ReadDocuments : ReadCommand<ReadDocuments.Input> {
    override val name: String = "read_documents"
    override val description: String = "Lists the project's other Markdown documents (README.md, docs/, AGENTS.md, a changelog; not the screenplay's pages), " +
        "or reads the ones named. They are the user's, so read one only when the request is about it."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        if (input.documents.isEmpty()) return CommandOutcome.Success(buildJsonArray { context.documents.paths().forEach { add(JsonPrimitive(it)) } })
        input.documents.firstNotNullOfOrNull { context.validateDocument(it) }?.let { return CommandOutcome.Rejected(it) }
        return withContext(context.ioDispatcher) {
            val texts = input.documents.associateWith { context.documents.read(it) }
            texts.entries.firstOrNull { it.value == null }?.let { return@withContext CommandOutcome.Rejected(Rejection(CommandProblem.DOCUMENT_UNREADABLE, it.key)) }
            CommandOutcome.Success(buildJsonObject { texts.forEach { (path, text) -> put(path, text) } })
        }
    }

    @Serializable
    public data class Input(
        @Description("The paths of the documents to read, or none to list every document's path.")
        @SerialName("documents") val documents: List<String> = emptyList(),
    )
}

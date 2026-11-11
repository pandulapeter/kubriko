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
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

public object ReadPages : ReadCommand<ReadPages.Input> {
    override val name: String = "read_pages"
    override val description: String = "Reads pages of the screenplay, the game's design in its own words, as Markdown: the ones named, or every page. " +
        "Their images are listed by path, not sent; view_page_image shows one."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome = withContext(context.ioDispatcher) {
        val scanned by lazy { context.screenplay.scanNames() }
        val names = input.pages.ifEmpty { scanned }
        val texts = names.associateWith { context.screenplay.read(it) }
        texts.entries.firstOrNull { it.value == null }?.let { return@withContext CommandOutcome.Rejected(missingPage(it.key, scanned)) }
        CommandOutcome.Success(
            buildJsonObject {
                texts.forEach { (page, text) ->
                    putJsonObject(page) {
                        put("text", text)
                        val images = imagesOf(page, checkNotNull(text))
                        if (images.isNotEmpty()) putJsonArray("images") { images.forEach(::add) }
                    }
                }
            },
        )
    }

    /**
     * The paths from the project's root of the images [text], the page [page], shows, in order.
     */
    private fun imagesOf(page: String, text: String) = PageLinks.linksOf(text).filter { it.isImage }.mapNotNull { PageLinks.resolve(PageNaming.pathOf(page), it.destination) }.distinct()

    @Serializable
    public data class Input(
        @Description("The names of the pages to read, or none for every page.")
        @SerialName("pages") val pages: List<String> = emptyList(),
    )
}

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.resources.knownResourcePaths
import com.pandulapeter.kubrikoCreator.foundation.NameMatching
import com.pandulapeter.kubrikoCreator.foundation.TextSnippet
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import kotlinx.coroutines.withContext

/**
 * Finds a query in the project's names and texts, for the Search panel and `search_project`: names as every search of
 * names matches them ([NameMatching]), and texts (descriptions, Log Blocks and texts set by Blocks, Kotlin Blocks and
 * Kotlin UI, screen texts, and the pages and documents) where they contain it, ignoring case, line by line.
 */
internal object ProjectSearch {

    /**
     * What [query] finds in [types], [names] (Scenes, files, and the like, each with its place), and [markdown] (each
     * page's and document's text by its path from the project's root): the names first, then the types' texts in their
     * order, then the Markdown, at most [limit] of them, with how many there are in all, the ones past the limit only
     * counted, so a short query that finds nearly every line builds no more than it lists.
     */
    fun search(
        query: String,
        types: List<BlockTypeDefinition>,
        names: List<Pair<String, UsagePlace>>,
        markdown: Map<String, String>,
        isSearchingNames: Boolean,
        limit: Int,
    ): SearchResults {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return SearchResults(emptyList(), 0)
        val hits = Hits(limit)
        if (isSearchingNames) {
            types.forEach { type ->
                hits.addName(type.name, UsagePlace.InModel(RelationPlace.Type(type.name)), trimmed)
                type.properties.forEach { property -> hits.addName(property.name, UsagePlace.InModel(RelationPlace.Type(type.name)), trimmed, "${type.name}.${property.name}") }
            }
            names.forEach { (name, place) -> hits.addName(name, place, trimmed) }
        }
        types.forEach { type -> hits.addTextsOf(type, trimmed) }
        markdown.keys.sorted().forEach { path -> hits.addLines(markdown.getValue(path), trimmed, UsagePlace.Markdown(path), SearchHit.Kind.MARKDOWN, isNumbered = true) }
        return SearchResults(hits.listed, hits.total)
    }

    /**
     * The first [limit] hits found, and how many there are in all.
     */
    private class Hits(
        private val limit: Int,
    ) {
        val listed = ArrayList<SearchHit>()
        var total = 0
            private set

        private val isFull get() = listed.size >= limit

        fun addName(name: String, place: UsagePlace, query: String, shown: String = name) {
            val match = NameMatching.match(name, query) ?: return
            total++
            if (isFull) return
            val offset = shown.length - name.length
            listed += SearchHit(place, SearchHit.Kind.NAME, shown, match.ranges.map { it.first + offset..it.last + offset })
        }

        fun addTextsOf(type: BlockTypeDefinition, query: String) {
            type.description?.let { addLines(it, query, UsagePlace.InModel(RelationPlace.Type(type.name)), SearchHit.Kind.DESCRIPTION) }
            type.events.forEachIndexed { eventIndex, event ->
                event.blocks.forEachIndexed { blockIndex, block ->
                    val place = UsagePlace.InModel(RelationPlace.Type(type.name, eventIndex, blockIndex))
                    when (block) {
                        is BlockDefinition.Log -> addLines(block.message, query, place, SearchHit.Kind.BLOCK_TEXT)
                        is BlockDefinition.SetText -> addLines(block.value, query, place, SearchHit.Kind.BLOCK_TEXT)
                        is BlockDefinition.Kotlin -> addLines(block.code, query, place, SearchHit.Kind.KOTLIN, isNumbered = true)
                        else -> Unit
                    }
                }
            }
            if (type is ManagerTypeDefinition) {
                type.texts.forEachIndexed { index, text -> addLines(text.text, query, UsagePlace.InModel(RelationPlace.Text(type.name, index)), SearchHit.Kind.SCREEN_TEXT) }
                type.kotlinUi?.let { addLines(it, query, UsagePlace.InModel(RelationPlace.KotlinUi(type.name)), SearchHit.Kind.KOTLIN, isNumbered = true) }
            }
        }

        /**
         * A hit for every line of [text] containing [query] ([TextSnippet]), with the line's number when [isNumbered]
         * (for code and Markdown, whose lines mean something).
         */
        fun addLines(text: String, query: String, place: UsagePlace, kind: SearchHit.Kind, isNumbered: Boolean = false) {
            text.lineSequence().forEachIndexed { index, line ->
                if (isFull) {
                    if (line.contains(query, ignoreCase = true)) total++
                } else {
                    TextSnippet.of(line, query)?.let { snippet ->
                        total++
                        listed += SearchHit(place, kind, snippet.text, snippet.ranges, (index + 1).takeIf { isNumbered })
                    }
                }
            }
        }
    }
}

/**
 * What [query] finds in the open project ([ProjectSearch]), its names too when [isSearchingNames], at most [limit] hits.
 * Reads the pages and documents, so it runs off the main thread.
 */
internal suspend fun CommandContext.searchProject(query: String, isSearchingNames: Boolean, limit: Int): SearchResults {
    val types = blockTypes?.types?.value.orEmpty()
    return withContext(ioDispatcher) {
        val markdown = screenplayTexts().markdown
        val names = if (!isSearchingNames) {
            emptyList()
        } else {
            scenes.names().map { it to UsagePlace.InModel(RelationPlace.Scene(it)) } +
                markdown.keys.sorted().map { path -> (PageNaming.nameOf(path) ?: path.substringAfterLast('/')) to UsagePlace.Markdown(path) } +
                knownResourcePaths().orEmpty().filter { ResourceKind.of(it) != null }.sorted().map { path -> path.substringAfterLast('/') to UsagePlace.ResourceFile(path) }
        }
        ProjectSearch.search(query, types, names, markdown, isSearchingNames, limit)
    }
}

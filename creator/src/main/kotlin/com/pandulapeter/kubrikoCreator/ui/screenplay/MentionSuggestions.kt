/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.DefinitionPaths
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.screenplay.Mention
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.ui.components.matchingOptions

/**
 * Something a page can mention: [title] is what the suggestion shows and the link's text, [detail] where it is when the
 * title alone doesn't say.
 */
internal data class MentionCandidate(
    val title: String,
    val detail: String?,
    val mention: Mention,
)

/**
 * An `@` typed to mention something: where the `@` is, and what was typed after it.
 */
internal data class MentionQuery(
    val start: Int,
    val text: String,
)

/**
 * Everything the page or document at [pagePath] can mention, in the order the sidebar shows them: the types, the
 * Scenes, the other pages, the documents, then the files.
 */
internal fun mentionCandidatesOf(
    blockTypes: List<BlockTypeDefinition>,
    resources: Map<ResourceGroup, List<ProjectResource>>,
    pageNames: List<String>,
    pagePath: String,
): List<MentionCandidate> = buildList {
    blockTypes.sortedBy { it.name.lowercase() }.forEach { type -> add(MentionCandidate(type.name, null, Mention.of(DefinitionPaths.definitionPath(type)))) }
    resources[ResourceGroup.SCENES].orEmpty().sortedBy { it.relativePath }.forEach { add(MentionCandidate(SceneStore.nameOf(it.file), null, Mention.of(it.relativePath))) }
    pageNames.filter { PageNaming.pathOf(it) != pagePath }.forEach { page -> add(MentionCandidate(page, null, Mention.of(PageNaming.pathOf(page)))) }
    listOf(ResourceGroup.DOCUMENTS, ResourceGroup.SPRITES, ResourceGroup.SOUNDS, ResourceGroup.MUSIC, ResourceGroup.KOTLIN).forEach { group ->
        resources[group].orEmpty().filter { it.relativePath != pagePath }.sortedBy { it.relativePath }.forEach { resource ->
            add(MentionCandidate(resource.name, resource.relativePath.substringBeforeLast('/', "").ifEmpty { null }, Mention.of(resource.relativePath)))
        }
    }
}.distinctBy { it.mention.path }

/**
 * The mention being typed at [value]'s cursor: an `@` that starts a word, followed by anything but spaces and brackets,
 * outside code; or null if there is none.
 */
internal fun mentionQueryOf(value: TextFieldValue): MentionQuery? {
    if (!value.selection.collapsed) return null
    val text = value.text
    val cursor = value.selection.start
    var index = cursor - 1
    while (index >= 0 && cursor - index <= MAXIMUM_QUERY_LENGTH) {
        val character = text[index]
        if (character == '@') break
        if (character.isWhitespace() || character in stoppers) return null
        index--
    }
    if (index < 0 || text[index] != '@') return null
    val before = text.getOrNull(index - 1)
    if (before != null && !before.isWhitespace() && before !in openers) return null
    val lineStart = text.lastIndexOf('\n', index - 1) + 1
    if (text.substring(lineStart, index).count { it == '`' } % 2 == 1) return null
    return MentionQuery(index, text.substring(index + 1, cursor))
}

/**
 * [value] with [query] replaced by a link to [candidate] written from the page at [pagePath], the cursor after it.
 */
internal fun withMention(value: TextFieldValue, query: MentionQuery, candidate: MentionCandidate, pagePath: String): TextFieldValue {
    val label = candidate.title.replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]")
    val link = "[$label](${PageLinks.destinationOf(pagePath, candidate.mention.path)})"
    val end = query.start + 1 + query.text.length
    return TextFieldValue(value.text.substring(0, query.start) + link + value.text.substring(end), TextRange(query.start + link.length))
}

/**
 * [value] with an `@` at its cursor, after a space when it would otherwise join the word before, so that the
 * suggestions open: what the toolbar's Mention does.
 */
internal fun withMentionStarted(value: TextFieldValue): TextFieldValue {
    val start = value.selection.min
    val before = value.text.getOrNull(start - 1)
    val inserted = if (before == null || before.isWhitespace() || before in openers) "@" else " @"
    return TextFieldValue(value.text.substring(0, start) + inserted + value.text.substring(value.selection.max), TextRange(start + inserted.length))
}

private const val MAXIMUM_QUERY_LENGTH = 60
private val stoppers = setOf('[', ']', '(', ')', '`')
private val openers = setOf('(', '"', '\'')

/**
 * A mention being typed and the candidates it matches, the best first.
 */
internal data class MentionSuggestions(
    val query: MentionQuery,
    val matches: List<MentionCandidate>,
)

/**
 * What to offer for the mention being typed at [value]'s cursor, or null if none is, if it matches nothing, or if its
 * suggestions were closed ([dismissedAt] is where its `@` is).
 */
internal fun mentionSuggestionsOf(value: TextFieldValue, candidates: List<MentionCandidate>, dismissedAt: Int?): MentionSuggestions? {
    val query = mentionQueryOf(value)?.takeIf { it.start != dismissedAt } ?: return null
    val matches = matchingOptions(query.text, candidates, titleOf = { it.title }, alsoMatching = { it.detail }).take(MAXIMUM_SUGGESTIONS)
    return if (matches.isEmpty()) null else MentionSuggestions(query, matches)
}

private const val MAXIMUM_SUGGESTIONS = 8

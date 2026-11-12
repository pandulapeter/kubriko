/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.foundation.NameMatch
import com.pandulapeter.kubrikoCreator.foundation.NameMatching
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.screenplay.Mention
import com.pandulapeter.kubrikoCreator.ui.screenplay.mentionCandidatesOf

/**
 * Something quick open offers: [title] is what it is found by, [detail] where it is when the title alone doesn't say.
 */
internal data class QuickOpenItem(
    val title: String,
    val detail: String?,
    val target: QuickOpenTarget,
)

/**
 * What choosing a [QuickOpenItem] opens.
 */
internal sealed interface QuickOpenTarget {

    /**
     * A type, Scene, page, document, sprite, sound, music, or Kotlin file.
     */
    data class File(
        val mention: Mention,
    ) : QuickOpenTarget

    /**
     * A Property, by the type declaring it.
     */
    data class Property(
        val type: String,
        val isOfManagerType: Boolean,
        val name: String,
    ) : QuickOpenTarget
}

/**
 * A [QuickOpenItem] that matches what is typed, with the letters of its title that matched.
 */
internal data class QuickOpenMatch(
    val item: QuickOpenItem,
    val titleRanges: List<IntRange>,
)

/**
 * Everything quick open offers, in the order the sidebar shows it: the types, the Scenes, the pages, the documents, the
 * files, and then every type's own Properties.
 */
internal fun quickOpenItemsOf(
    blockTypes: List<BlockTypeDefinition>,
    resources: Map<ResourceGroup, List<ProjectResource>>,
    pageNames: List<String>,
): List<QuickOpenItem> = mentionCandidatesOf(blockTypes, resources, pageNames, pagePath = "").map { candidate ->
    QuickOpenItem(candidate.title, candidate.detail, QuickOpenTarget.File(candidate.mention))
} + blockTypes.sortedBy { it.name.lowercase() }.flatMap { type ->
    type.properties.map { property ->
        QuickOpenItem(property.name, type.name, QuickOpenTarget.Property(type.name, type is ManagerTypeDefinition, property.name))
    }
}

/**
 * The [items] matching [query], at most [limit] of them: those whose title matches ([NameMatching]) the closest first,
 * then those only whose detail does, otherwise in their order. A blank query matches the first [limit] items.
 */
internal fun quickOpenMatchesOf(query: String, items: List<QuickOpenItem>, limit: Int = QUICK_OPEN_LIMIT): List<QuickOpenMatch> {
    if (query.isBlank()) return items.take(limit).map { QuickOpenMatch(it, emptyList()) }
    val byTitle = ArrayList<Pair<QuickOpenItem, NameMatch>>()
    val byDetail = ArrayList<Pair<QuickOpenItem, NameMatch>>()
    items.forEach { item ->
        val titleMatch = NameMatching.match(item.title, query)
        when {
            titleMatch != null -> byTitle += item to titleMatch
            else -> item.detail?.let { NameMatching.match(it, query) }?.let { byDetail += item to it }
        }
    }
    byTitle.sortBy { it.second }
    byDetail.sortBy { it.second }
    return byTitle.map { (item, match) -> QuickOpenMatch(item, match.ranges) }.plus(byDetail.map { (item, _) -> QuickOpenMatch(item, emptyList()) }).take(limit)
}

/**
 * How many matches quick open lists at most: enough to scroll through, few enough to read.
 */
internal const val QUICK_OPEN_LIMIT = 50

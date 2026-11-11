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

import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.SimilarNames
import com.pandulapeter.kubrikoCreator.commands.toRejection
import com.pandulapeter.kubrikoCreator.screenplay.Mention
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.PageText
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStyle

/**
 * How long a page can be: pages live in git and in the AI's context, so a longer one is split by topic.
 */
internal const val PAGE_MAXIMUM_LENGTH = 30_000

/**
 * Why there is no page called [name], or null if there is one.
 */
internal fun CommandContext.validatePage(name: String): Rejection? = if (screenplay.existingName(name) == name) null else missingPage(name, screenplay.names())

/**
 * Why there is no page called [name] among [names], naming the ones called similarly.
 */
internal fun missingPage(name: String, names: List<String>) = SimilarNames.rejection(CommandProblem.PAGE_MISSING, CommandProblem.PAGE_MISSING_SIMILAR, name, names, name)

/**
 * Why the page or document [file], which holds [text], has no section headed [heading], naming the sections it has.
 */
internal fun missingSection(file: String, heading: String, text: String): Rejection {
    val headings = PageText.sectionsOf(text).map { it.heading }.distinct()
    return if (headings.isEmpty()) {
        Rejection(CommandProblem.PAGE_SECTION_MISSING, file, heading)
    } else {
        Rejection(CommandProblem.PAGE_SECTION_MISSING_LISTED, file, heading, headings.joinToString())
    }
}

/**
 * Why [name] can't be the name of a new page (or of [renamed] moved), or null if it can.
 */
internal fun CommandContext.validateNewPageName(name: String, renamed: String? = null): Rejection? = validatePageName(name) ?: when {
    screenplay.existingName(name)?.let { it != renamed } == true -> Rejection(CommandProblem.PAGE_TAKEN, name)
    else -> null
}

/**
 * Why [name] can't be a page's name (or a folder of pages), or null if it can.
 */
internal fun validatePageName(name: String): Rejection? = PageNaming.validate(name)?.toRejection()

internal fun validatePageText(text: String): Rejection? = if (text.length > PAGE_MAXIMUM_LENGTH) Rejection(CommandProblem.PAGE_TOO_LONG, PAGE_MAXIMUM_LENGTH.toString()) else null

internal const val PAGE_NAME_DESCRIPTION = "The page: its path in the screenplay folder without .md, like levels/cave; README is the first page."

/**
 * Why the AI may not write [text] into the page at [pagePath]: it breaks the screenplay's writing rules
 * ([ScreenplayStyle]), numbers included, which are the values of the Properties of the types it mentions.
 */
internal fun CommandContext.validatePageStyle(pagePath: String, text: String, isWholePage: Boolean): Rejection? {
    val problems = ScreenplayStyle.problemsOf(text, pagePath, tunableNumbersOf(pagePath, text), isWholePage)
    return if (problems.isEmpty()) null else Rejection(CommandProblem.SCREENPLAY_STYLE, problems.joinToString("; "))
}

/**
 * The values of the number Properties of each type [text], written in the page at [pagePath], mentions, as prose would
 * write them, by the path of the type's file; 0 and 1 are left out, since they are as often words as numbers.
 */
private fun CommandContext.tunableNumbersOf(pagePath: String, text: String): Map<String, Set<String>> = PageLinks.linksOf(text)
    .mapNotNull { PageLinks.resolve(pagePath, it.destination) }
    .distinct()
    .mapNotNull { path ->
        val name = when (val mention = Mention.of(path)) {
            is Mention.ActorType -> mention.name
            is Mention.ManagerType -> mention.name
            else -> null
        }
        val numbers = name?.let { blockTypes?.get(it) }?.properties?.filterIsInstance<PropertyDefinition.Number>()?.map { it.initialValue }
            ?.filter { it != 0f && it != 1f }
            ?.mapTo(HashSet()) { value -> if (value % 1f == 0f) value.toLong().toString() else value.toString() }
        numbers?.takeIf { it.isNotEmpty() }?.let { path to it }
    }
    .toMap()

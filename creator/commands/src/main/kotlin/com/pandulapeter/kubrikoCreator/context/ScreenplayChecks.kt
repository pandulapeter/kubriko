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

import com.pandulapeter.kubrikoCreator.screenplay.Mention
import com.pandulapeter.kubrikoCreator.screenplay.MentionIndex
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.PageText

/**
 * Works out what about the screenplay is worth a look: mentions of what is gone, and, as reminders that can be turned
 * off, the cast, crew, and Scenes no page mentions, pages off the shape a page has, and Planned items whose subject
 * already exists.
 */
internal object ScreenplayChecks {
    private const val ITEM_LENGTH = 80

    /**
     * @param pages Every page's text, by its name.
     * @param index Which of [pages] mention what.
     * @param subjects The path from the project's root of each Actor type's, Manager type's, and Scene's file.
     * @param exists Whether a file is at a path from the project's root.
     * @param areRemindersShown Whether the reminders are on ([com.pandulapeter.kubrikoCreator.screenplay.ScreenplaySettings]).
     */
    fun of(pages: Map<String, String>, index: MentionIndex, subjects: Map<RelationSubject, String>, exists: (String) -> Boolean, areRemindersShown: Boolean): List<HealthFinding> = buildList {
        val names = pages.keys.sortedWith(compareBy<String> { it != PageNaming.FIRST_PAGE }.thenBy { it.lowercase() })
        names.forEach { name ->
            val pagePath = PageNaming.pathOf(name)
            PageLinks.linksOf(checkNotNull(pages[name])).filter { !it.isImage }.mapNotNullTo(LinkedHashSet()) { PageLinks.resolve(pagePath, it.destination) }
                .filter { Mention.of(it) !is Mention.OtherFile && !exists(it) }
                .forEach { add(HealthFinding(HealthFinding.Kind.MENTION_MISSING, RelationSubject.Page(name), usage = it)) }
        }
        if (!areRemindersShown || pages.isEmpty()) return@buildList
        subjects.filterValues { index.pagesMentioning(it).isEmpty() }.keys.forEach { add(HealthFinding(HealthFinding.Kind.UNMENTIONED, it)) }
        names.forEach { name ->
            val text = checkNotNull(pages[name])
            if (PageText.titleOf(text) == null) add(HealthFinding(HealthFinding.Kind.PAGE_WITHOUT_TITLE, RelationSubject.Page(name)))
            if (PageText.summaryOf(text) == null) add(HealthFinding(HealthFinding.Kind.PAGE_WITHOUT_SUMMARY, RelationSubject.Page(name)))
            plannedItemsMentioningWhatExists(name, text, subjects.values.toSet()).forEach { item ->
                add(HealthFinding(HealthFinding.Kind.PLANNED_MAY_BE_BUILT, RelationSubject.Page(name), detail = item))
            }
        }
    }

    private fun plannedItemsMentioningWhatExists(name: String, text: String, subjectPaths: Set<String>): List<String> {
        val planned = PageText.sectionsOf(text).firstOrNull { it.level == 2 && it.heading.equals(PageText.PLANNED, ignoreCase = true) } ?: return emptyList()
        val lines = text.lines()
        return planned.lines.drop(1).map { lines[it].trim() }.filter { it.startsWith("- ") || it.startsWith("* ") }.filter { item ->
            PageLinks.linksOf(item).any { link -> PageLinks.resolve(PageNaming.pathOf(name), link.destination) in subjectPaths }
        }.map { it.drop(2).let { item -> if (item.length > ITEM_LENGTH) item.take(ITEM_LENGTH).trimEnd() + "…" else item } }
    }
}

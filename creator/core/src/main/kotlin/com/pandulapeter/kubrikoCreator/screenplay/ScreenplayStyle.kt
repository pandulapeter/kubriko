/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * The screenplay's writing rules: a page describes the game as it is, for the player, in the game's own words. Every
 * page the AI writes is checked against them (and refused until it keeps them); the user sees them only as quiet hints,
 * since the design is theirs.
 */
public object ScreenplayStyle {

    /**
     * How long a page grows before it is split, in characters.
     */
    public const val LENGTH_LIMIT: Int = 6_000

    /**
     * How long a Planned item grows, in characters.
     */
    public const val PLANNED_ITEM_LIMIT: Int = 140

    public enum class Rule(
        override val english: String,
    ) : DomainText {
        HISTORY("Describe the game as it is, in the present tense, without its history."),
        NARRATION("Leave out the conversation and the work; say what the game is."),
        CODE("Describe the design, not the code: link to what does it instead."),
        COPIED_NUMBER("Name this number by a mention of its Property rather than copying it, so it can't drift."),
        LONG_PLANNED_ITEM("Keep each Planned item short: one line, or a subheading."),
        PROSE_IN_PLANNED("Write Planned as a list of short items."),
        TOO_LONG("Split this page: it is past the length one page holds."),
    }

    /**
     * Where a text breaks [rule]: its line, from 1, and the words that do.
     */
    public data class Problem(
        val rule: Rule,
        val line: Int,
        val excerpt: String,
    ) {
        override fun toString(): String = "line $line, \"$excerpt\": ${rule.english}"
    }

    /**
     * Where [text] breaks the rules.
     *
     * @param pagePath The path of the page from the project's root, which its mentions are read from.
     * @param tunableNumbers The values of each mentioned type's Properties as they would be written (`240`), by the path of
     *   the type's file: a line mentioning the type is to name such a number by its Property's mention.
     * @param isWholePage Whether [text] is a whole page rather than a section of one, which only a whole page's length
     *   is checked for.
     */
    public fun problemsOf(text: String, pagePath: String? = null, tunableNumbers: Map<String, Set<String>> = emptyMap(), isWholePage: Boolean = true): List<Problem> {
        val problems = ArrayList<Problem>()
        val lines = text.lines()
        val planned = PageText.plannedSectionOf(text)?.lines
        var isInCode = false
        val numberPatterns = tunableNumbers.values.flatten().distinct().associateWith { number -> Regex("(?<![\\w.])" + Regex.escape(number) + "(?![\\w]|\\.\\d)") }
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            if (line.trimStart().startsWith("```") || line.trimStart().startsWith("~~~")) {
                if (!isInCode) problems += Problem(Rule.CODE, lineNumber, line.trim())
                isInCode = !isInCode
                return@forEachIndexed
            }
            if (isInCode) return@forEachIndexed
            val prose = proseOf(line)
            history.find(prose)?.let { problems += Problem(Rule.HISTORY, lineNumber, it.value) }
            narration.find(prose)?.let { problems += Problem(Rule.NARRATION, lineNumber, it.value) }
            copiedNumberOf(line, prose, pagePath, tunableNumbers, numberPatterns)?.let { problems += Problem(Rule.COPIED_NUMBER, lineNumber, it) }
            if (planned != null && index in planned && index != planned.first && line.isNotBlank()) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("#") -> Unit
                    listItem.matches(trimmed) -> if (trimmed.length > PLANNED_ITEM_LIMIT) problems += Problem(Rule.LONG_PLANNED_ITEM, lineNumber, trimmed.take(EXCERPT_LENGTH))
                    !line.startsWith(" ") -> problems += Problem(Rule.PROSE_IN_PLANNED, lineNumber, trimmed.take(EXCERPT_LENGTH))
                }
            }
        }
        if (isWholePage && text.length > LENGTH_LIMIT) problems += Problem(Rule.TOO_LONG, lines.size, "${text.length} characters")
        return problems
    }

    /**
     * The first number on [line] that is the value of a Property of a type the line mentions, written as prose ([prose],
     * outside its links) rather than as the Property's mention.
     */
    private fun copiedNumberOf(line: String, prose: String, pagePath: String?, tunableNumbers: Map<String, Set<String>>, patterns: Map<String, Regex>): String? {
        if (pagePath == null || tunableNumbers.isEmpty()) return null
        val numbers = PageLinks.linksOf(line).mapNotNull { PageLinks.resolve(pagePath, it.destination) }.flatMap { tunableNumbers[it].orEmpty() }
        val unlinked = prose.replace(link, " ")
        return numbers.firstOrNull { patterns.getValue(it).containsMatchIn(unlinked) }
    }

    /**
     * [line] without its code spans and link destinations, which hold names and paths rather than prose.
     */
    private fun proseOf(line: String) = line.replace(codeSpan, " ").replace(linkDestination, "]")

    private const val EXCERPT_LENGTH = 60
    private val codeSpan = Regex("`[^`]*`")
    private val linkDestination = Regex("]\\([^)]*\\)")
    private val link = Regex("!?\\[[^\\]]*]")
    private val listItem = Regex("([-*+]|\\d+[.)])\\s.*")
    /**
     * Words only history uses: "now" and "no longer" also describe the game as it is ("the door is now open"), so they
     * aren't among them.
     */
    private val history = Regex(
        "\\b(used to|previously|formerly|originally|(was|were|has been|have been|got) (changed|fixed|renamed|removed|added|replaced)|" +
            "fixed (a|an|the) (bug|crash|problem|issue))\\b|\\b(19|20)\\d{2}-\\d{2}-\\d{2}\\b|" +
            "\\b(January|February|March|April|May|June|July|August|September|October|November|December) \\d{1,2}(st|nd|rd|th)?,? (19|20)\\d{2}\\b",
        RegexOption.IGNORE_CASE,
    )
    private val narration = Regex(
        "\\b(I|we) (added|changed|removed|made|decided|fixed|created|updated|renamed|moved|built|wrote|implemented|set up|tweaked)\\b|" +
            "\\b(I've|I have|we've|we have) \\w+ed\\b|\\bas (you )?(requested|asked)\\b|\\bper your request\\b",
        RegexOption.IGNORE_CASE,
    )
}

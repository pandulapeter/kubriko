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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectSearchTest {
    private val ball = ActorTypeDefinition(
        name = "Ball",
        description = "Bounces off every wall",
        properties = listOf(PropertyDefinition.Number("wallHits")),
        events = listOf(
            EventDefinition(EventType.EDGE_TOUCHED, blocks = listOf(BlockDefinition.Log("Hit a WALL"), BlockDefinition.Kotlin("val a = 1\nprintln(\"wall wall\")"))),
        ),
    )
    private val game = ManagerTypeDefinition("Game", texts = listOf(ScreenTextDefinition("Walls left: {walls}")), kotlinUi = "Text(\"Wall\")")
    private val markdown = mapOf("screenplay/story.md" to "# Story\n\nThe ball breaks every wall.", "NOTES.md" to "nothing here")

    private fun search(query: String, isSearchingNames: Boolean = true, limit: Int = 100) = ProjectSearch.search(
        query = query,
        types = listOf(ball, game),
        names = listOf("walls" to UsagePlace.InModel(RelationPlace.Scene("walls"))),
        markdown = markdown,
        isSearchingNames = isSearchingNames,
        limit = limit,
    )

    @Test
    fun namesComeFirstThenEveryTypesTextsThenTheMarkdownEachLineWithItsMatchesMarked() = assertEquals(
        listOf(
            "Ball (name): Ball.wallHits",
            "Scene walls (name): walls",
            "Ball (description): Bounces off every wall",
            "Ball#0.0 (Block text): Hit a WALL",
            "Ball#0.1:2 (Kotlin): println(\"wall wall\")",
            "Game text 0 (screen text): Walls left: {walls}",
            "Game Kotlin UI:1 (Kotlin): Text(\"Wall\")",
            "screenplay/story.md:3 (Markdown): The ball breaks every wall.",
        ),
        search("wall").hits.map { it.toString() },
    )

    @Test
    fun everyMatchOfALineIsMarkedAndNamesAreMarkedWhereTheyMatched() {
        val hits = search("wall").hits
        assertEquals(listOf(9..12, 14..17), hits.single { it.kind == SearchHit.Kind.KOTLIN && it.line == 2 }.ranges)
        assertEquals(listOf(5..8), hits.first().ranges, "Ball.wallHits, from the Property's own name")
    }

    @Test
    fun textsAloneCanBeSearchedAndABlankQueryFindsNothing() {
        assertEquals(SearchHit.Kind.DESCRIPTION, search("wall", isSearchingNames = false).hits.first().kind)
        assertEquals(SearchResults(emptyList(), 0), search("  "))
    }

    @Test
    fun theHitsStopAtTheLimitButAreCountedInAll() {
        val results = search("wall", limit = 3)
        assertEquals(3, results.hits.size)
        assertEquals(8, results.total)
    }

    @Test
    fun aLongLineIsShortenedAroundItsFirstMatch() {
        val long = "x".repeat(200) + "needle" + "y".repeat(200)
        val hit = ProjectSearch.search("needle", emptyList(), emptyList(), mapOf("a.md" to long), isSearchingNames = false, limit = 10).hits.single()
        assertEquals(true, hit.text.startsWith("…") && hit.text.endsWith("…"))
        assertEquals("needle", hit.text.substring(hit.ranges.single().first, hit.ranges.single().last + 1))
    }
}

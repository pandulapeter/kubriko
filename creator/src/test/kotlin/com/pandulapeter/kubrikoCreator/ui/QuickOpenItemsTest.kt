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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.screenplay.Mention
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickOpenItemsTest {
    private val types = listOf(
        ActorTypeDefinition(name = "Brick", properties = listOf(PropertyDefinition.Number("hits"))),
        ManagerTypeDefinition(name = "Game", properties = listOf(PropertyDefinition.Number("score"))),
    )
    private val resources = mapOf(
        ResourceGroup.SCENES to listOf(resource(ResourceGroup.SCENES, "src/commonMain/composeResources/files/scenes/levels/Cave.json")),
        ResourceGroup.SPRITES to listOf(resource(ResourceGroup.SPRITES, "src/commonMain/composeResources/drawable/brick.png")),
        ResourceGroup.SOUNDS to listOf(resource(ResourceGroup.SOUNDS, "src/commonMain/composeResources/files/sounds/bounce.wav")),
        ResourceGroup.KOTLIN to listOf(resource(ResourceGroup.KOTLIN, "src/commonMain/kotlin/game/Game.kt")),
    )
    private val items = quickOpenItemsOf(types, resources, pageNames = listOf("story"))

    @Test
    fun everythingIsOfferedInTheSidebarsOrderWithTheTypesPropertiesLast() = assertEquals(
        listOf("Brick", "Game", "levels/Cave", "story", "brick.png", "bounce.wav", "Game.kt", "hits", "score"),
        items.map { it.title },
    )

    @Test
    fun aPropertyNamesItsTypeAndWhetherThatIsAManagerType() = assertEquals(
        listOf(QuickOpenTarget.Property("Brick", isOfManagerType = false, name = "hits"), QuickOpenTarget.Property("Game", isOfManagerType = true, name = "score")),
        items.map { it.target }.filterIsInstance<QuickOpenTarget.Property>(),
    )

    @Test
    fun titlesMatchingTheClosestComeFirstThenThoseOnlyWhoseDetailMatches() {
        val matches = quickOpenMatchesOf("game", items)
        assertEquals(listOf("Game", "Game.kt", "score"), matches.map { it.item.title })
        assertEquals(listOf(listOf(0..3), listOf(0..3), emptyList()), matches.map { it.titleRanges })
    }

    @Test
    fun lettersInOrderFindNamesAsInTheSidebar() = assertEquals(listOf("Brick", "brick.png", "hits"), quickOpenMatchesOf("brk", items).map { it.item.title })

    @Test
    fun aBlankQueryListsTheFirstItemsUpToTheLimit() {
        assertEquals(items.take(3).map { it.title }, quickOpenMatchesOf(" ", items, limit = 3).map { it.item.title })
        assertEquals(2, quickOpenMatchesOf("b", items, limit = 2).size)
    }

    @Test
    fun aFileIsOfferedByWhatItIs() = assertEquals(
        Mention.Sprite("src/commonMain/composeResources/drawable/brick.png"),
        (items.single { it.title == "brick.png" }.target as QuickOpenTarget.File).mention,
    )

    private fun resource(group: ResourceGroup, path: String) = ProjectResource(group, File(path), path)

    @Test
    fun theLitRowFollowsItsItemWhereverTheListMovesIt() {
        val brick = items.single { it.title == "Brick" }.target
        val game = items.single { it.title == "Game" }.target
        val matches = quickOpenMatchesOf("", items)
        assertEquals(0, litIndexOf(matches, null))
        assertEquals(1, litIndexOf(matches, game))
        assertEquals(0, litIndexOf(matches.drop(1), game), "Game moved up when Brick went")
        assertEquals(0, litIndexOf(matches.drop(1), brick), "an item the list no longer has lights the closest match")
    }
}

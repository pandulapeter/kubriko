/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.PropertyReference
import kotlin.test.Test
import kotlin.test.assertEquals

class PropertyChoiceTest {

    @Test
    fun blocksOfferOnlyThePropertiesOfTheKindTheyNeed() {
        val scope = BlockScope("Player", properties = listOf(PropertyDefinition.Number("score"), PropertyDefinition.Text("title"), PropertyDefinition.Number("lives")))
        assertEquals(listOf(PropertyReference("score", PropertyKind.NUMBER), PropertyReference("lives", PropertyKind.NUMBER)), propertyChoices(scope, PropertyKind.NUMBER))
        assertEquals(listOf(PropertyReference("title", PropertyKind.TEXT)), propertyChoices(scope, PropertyKind.TEXT))
        assertEquals(emptyList(), propertyChoices(scope, PropertyKind.TOGGLE))
    }

    @Test
    fun theTypesOwnPropertiesComeBeforeThoseOfEachOtherManager() {
        val game = ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Number("score"), PropertyDefinition.Toggle("isOver")))
        val level = ManagerTypeDefinition("Level", properties = listOf(PropertyDefinition.Number("number")))
        val inAnActor = BlockScope("Player", properties = listOf(PropertyDefinition.Number("lives")), managerTypes = listOf(game, level))
        assertEquals(
            listOf(PropertyReference("lives", PropertyKind.NUMBER), PropertyReference("score", PropertyKind.NUMBER, "Game"), PropertyReference("number", PropertyKind.NUMBER, "Level")),
            propertyChoices(inAnActor, PropertyKind.NUMBER),
        )
        val inAManager = BlockScope("Game", BlockTypeKind.MANAGER, game.properties, managerTypes = listOf(game, level))
        assertEquals(
            listOf(PropertyReference("score", PropertyKind.NUMBER), PropertyReference("number", PropertyKind.NUMBER, "Level")),
            propertyChoices(inAManager, PropertyKind.NUMBER),
        )
    }
}

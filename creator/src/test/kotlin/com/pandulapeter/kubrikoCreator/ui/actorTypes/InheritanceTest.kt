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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import kotlin.test.Test
import kotlin.test.assertEquals

class InheritanceTest {
    private val enemy = ActorTypeDefinition(
        name = "Enemy",
        properties = listOf(PropertyDefinition.Number("hp"), PropertyDefinition.Text("cry")),
        events = listOf(EventDefinition(EventType.EVERY_FRAME), EventDefinition(EventType.ADDED_TO_SCENE)),
    )
    private val bat = ActorTypeDefinition(name = "Bat", parent = "Enemy", properties = listOf(PropertyDefinition.Number("hp")), events = listOf(EventDefinition(EventType.EVERY_FRAME)))
    private val vampire = ActorTypeDefinition(name = "Vampire", parent = "Bat")
    private val game = ManagerTypeDefinition(name = "Game")
    private val types = listOf(enemy, bat, vampire, game)

    @Test
    fun aChildSeesWhatItInheritsAndDoesNotReplace() {
        assertEquals(
            Inheritance(
                properties = listOf(PropertyDefinition.Text("cry")),
                events = listOf(EventDefinition(EventType.ADDED_TO_SCENE)),
                parentChoices = listOf("Enemy"),
            ),
            Inheritance.of(bat, types),
        )
    }

    @Test
    fun aTypeCanNotInheritFromItselfOrItsDescendants() {
        assertEquals(emptyList(), Inheritance.of(enemy, types).parentChoices)
        assertEquals(listOf("Enemy", "Bat"), Inheritance.of(vampire, types).parentChoices)
        assertEquals(Inheritance(), Inheritance.of(game, types))
    }
}

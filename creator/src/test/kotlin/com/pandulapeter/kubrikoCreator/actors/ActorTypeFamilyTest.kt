/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActorTypeFamilyTest {
    private val hitWall = EventDefinition(EventType.COLLISION, otherActorType = "Wall", blocks = listOf(BlockDefinition.Stop))
    private val enemy = ActorTypeDefinition(
        name = "Enemy",
        properties = listOf(PropertyDefinition.Number("hp", initialValue = 3f), PropertyDefinition.Text("cry")),
        events = listOf(EventDefinition(EventType.EVERY_FRAME), hitWall),
    )
    private val flying = ActorTypeDefinition(name = "Flying", parent = "Enemy", properties = listOf(PropertyDefinition.Number("height")))
    private val bat = ActorTypeDefinition(
        name = "Bat",
        parent = "Flying",
        properties = listOf(PropertyDefinition.Number("hp", initialValue = 1f), PropertyDefinition.Toggle("cry")),
        events = listOf(hitWall.copy(blocks = listOf(BlockDefinition.RemoveSelf))),
    )
    private val wall = ActorTypeDefinition(name = "Wall", parent = "Ghost")
    private val family = ActorTypeFamily(listOf(enemy, flying, bat, wall))

    @Test
    fun parentsMakeChainsAndAParentThatDoesNotExistIsNone() {
        assertEquals(listOf("Flying", "Enemy"), family.ancestorsOf("Bat"))
        assertEquals(listOf("Flying", "Bat"), family.descendantsOf("Enemy"))
        assertEquals(listOf("Enemy", "Flying", "Bat"), family.familyOf("Enemy"))
        assertEquals("Enemy", family.rootOf("Bat"))
        assertTrue(family.hasChildren("Flying"))
        assertFalse(family.hasChildren("Bat"))
        assertNull(family.parentOf("Wall"))
    }

    @Test
    fun aCycleCountsAsNoParent() {
        val cycle = ActorTypeFamily(listOf(ActorTypeDefinition(name = "A", parent = "B"), ActorTypeDefinition(name = "B", parent = "A")))
        assertTrue(cycle.ancestorsOf("A").size <= 1)
        assertTrue(ActorTypeFamily.isAncestor("A", "B", mapOf("A" to "B", "B" to "A")))
        assertFalse(ActorTypeFamily.isAncestor("Bat", "Enemy", mapOf("Bat" to "Flying", "Flying" to "Enemy")))
    }

    @Test
    fun childrenInheritPropertiesAndChangeOnlyTheirInitialValues() {
        // A Property of another kind with an inherited one's name is left out, since it couldn't be generated.
        assertEquals(
            listOf(PropertyDefinition.Number("hp", initialValue = 1f), PropertyDefinition.Text("cry"), PropertyDefinition.Number("height")),
            family.propertiesOf("Bat"),
        )
    }

    @Test
    fun anEventOfTheSameKindReplacesTheParentsOne() {
        assertEquals(listOf(EventDefinition(EventType.EVERY_FRAME), hitWall), family.eventsOf("Flying"))
        assertEquals(listOf(EventDefinition(EventType.EVERY_FRAME), hitWall.copy(blocks = listOf(BlockDefinition.RemoveSelf))), family.eventsOf("Bat"))
    }
}

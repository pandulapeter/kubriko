/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class GeneratedFamiliesTest {
    private val enemy = ActorTypeDefinition(
        name = "Enemy",
        properties = listOf(PropertyDefinition.Number("hp", initialValue = 3f)),
        events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeNumber("hp", by = -1f)))),
    )
    private val children = List(30) { index ->
        ActorTypeDefinition(
            name = "Enemy$index",
            parent = "Enemy",
            events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Wall", blocks = listOf(BlockDefinition.RunParentEvent, BlockDefinition.Stop))),
        )
    }
    private val wall = ActorTypeDefinition(name = "Wall", parent = "Brick")
    private val brick = ActorTypeDefinition(name = "Brick")
    private val actorTypes = listOf(enemy) + children + listOf(wall, brick)
    private val family = ActorTypeFamily(actorTypes)
    private val collidableActorTypeNames = PluginManagers.of(actorTypes, family = family).collidableActorTypeNames

    @Test
    fun eachFamilyIsBuiltOnceForAllOfItsMembers() {
        val families = GeneratedFamilies(actorTypes, emptyList(), collidableActorTypeNames, family)
        val first = families.of("Enemy7")
        assertSame(first, families.of("Enemy"))
        assertSame(first, families.of("Enemy29"))
        assertNotSame(first, families.of("Wall"))
        assertSame(families.of("Wall"), families.of("Brick"))
    }

    @Test
    fun sharingTheFamiliesGeneratesWhatEachTypeGeneratesAlone() {
        val families = GeneratedFamilies(actorTypes, emptyList(), collidableActorTypeNames, family)
        actorTypes.forEach { type ->
            assertEquals(
                generate(type, GeneratedFamilies(actorTypes, emptyList(), collidableActorTypeNames, family)),
                generate(type, families),
                type.name,
            )
        }
    }

    private fun generate(definition: ActorTypeDefinition, families: GeneratedFamilies) = ActorTypeCodeGenerator.generateActorType(
        definition = definition,
        packageName = "com.example.game",
        knownActorTypeNames = actorTypes.map { it.name },
        collidableActorTypeNames = collidableActorTypeNames,
        actorTypes = actorTypes,
        families = families,
    )
}

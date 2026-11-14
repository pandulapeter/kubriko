/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.store

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyNaming
import com.pandulapeter.kubrikoCreator.actors.codegen.SceneBoundsCode
import com.pandulapeter.kubrikoCreator.project.ProjectFormat
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SceneLeavingMigrationTest {
    private val root = testDirectory()

    init {
        File(root, ".kubriko/project.json").apply {
            parentFile.mkdirs()
            writeText("""{ "packageName": "com.example.game", "formatVersion": 6 }""")
        }
    }

    private val store = assertNotNull(BlockTypeStore.open(root))

    private fun migratedTypes(): Map<String, Any> {
        assertEquals(ProjectFormatStatus.Writable, ProjectFormat(root, creatorVersion = "1.0.0").prepare())
        val loaded = assertNotNull(BlockTypeStore.open(root)).load()
        assertEquals(emptyList(), loaded.unreadableFiles)
        return loaded.types.associateBy { it.name }
    }

    @Test
    fun everyActorTypeThatReactsToTheEdgeEventStaysInsideSoItBehavesTheSame() {
        val edge = listOf(EventDefinition(EventType.EDGE_TOUCHED, blocks = listOf(BlockDefinition.BounceOffEdge)))
        store.save(ActorTypeDefinition(name = "Ball", events = edge))
        store.save(ActorTypeDefinition(name = "Brick", events = listOf(EventDefinition(EventType.EVERY_FRAME))))
        store.save(ActorTypeDefinition(name = "Enemy", events = edge))
        store.save(ActorTypeDefinition(name = "Bat", parent = "Enemy"))
        store.save(ManagerTypeDefinition(name = "Game", events = listOf(EventDefinition(EventType.EDGE_TOUCHED))))
        val types = migratedTypes()
        assertEquals(true, (types.getValue("Ball") as ActorTypeDefinition).staysInsideScene)
        assertEquals(false, (types.getValue("Brick") as ActorTypeDefinition).staysInsideScene)
        assertEquals(true, (types.getValue("Enemy") as ActorTypeDefinition).staysInsideScene)
        assertEquals(true, (types.getValue("Bat") as ActorTypeDefinition).staysInsideScene, "an inherited edge Event pushed it back too")
    }

    @Test
    fun everyMemberOfAFamilyStaysInsideWhenAnyOfThemReactsToTheEdgeEventSinceTheirBaseClassPushedThemAllBack() {
        store.save(ActorTypeDefinition(name = "Enemy"))
        store.save(ActorTypeDefinition(name = "Bat", parent = "Enemy", events = listOf(EventDefinition(EventType.EDGE_TOUCHED))))
        store.save(ActorTypeDefinition(name = "Ghost", parent = "Enemy"))
        store.save(ActorTypeDefinition(name = "Wall"))
        val types = migratedTypes()
        listOf("Enemy", "Bat", "Ghost").forEach { assertEquals(true, (types.getValue(it) as ActorTypeDefinition).staysInsideScene, it) }
        assertEquals(false, (types.getValue("Wall") as ActorTypeDefinition).staysInsideScene)
    }

    @Test
    fun thePropertiesNamedLikeWhatTheGeneratedClassesKeepAboutTheBoundsAreRenamed() {
        store.save(ActorTypeDefinition(name = "Ball", properties = listOf(PropertyDefinition.Toggle("wasInsideScene"), PropertyDefinition.Toggle("staysInsideScene"))))
        val ball = migratedTypes().getValue("Ball") as ActorTypeDefinition
        assertEquals(listOf(PropertyDefinition.Toggle("wasInsideScene2"), PropertyDefinition.Toggle("staysInsideScene2")), ball.properties)
    }

    @Test
    fun whatTheGeneratedClassesKeepAboutTheBoundsCanBeNoPropertysName() {
        (SceneBoundsCode.WAS_TOUCHING_FIELDS + SceneBoundsCode.WAS_INSIDE_FIELD + SceneBoundsCode.STAYS_INSIDE_FIELD).forEach { name ->
            assertEquals(NameProblem.PROPERTY_RESERVED, PropertyNaming.validate(name, emptyList(), BlockTypeKind.ACTOR), name)
        }
    }
}

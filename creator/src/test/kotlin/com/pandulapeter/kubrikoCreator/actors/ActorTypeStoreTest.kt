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

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActorTypeStoreTest {
    private val root = createTempDirectory().toFile()
    private val generated = File(root, "src/commonMain/kotlin/com/example/game/generated")
    private val ball = ActorTypeDefinition(
        name = "Ball",
        events = listOf(EventDefinition(EventType.EDGE_TOUCHED, blocks = listOf(BlockDefinition.BounceOffEdge))),
    )

    private fun optIn(settings: String = """{ "packageName": "com.example.game" }""") = File(root, ".kubriko/project.json").apply {
        parentFile.mkdirs()
        writeText(settings)
    }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun aProjectWithoutSettingsHasNoStore() {
        assertNull(ActorTypeStore.open(root))
    }

    @Test
    fun savingWritesTheDefinitionItsClassAndTheRegistry() {
        optIn()
        val store = assertNotNull(ActorTypeStore.open(root))
        store.save(ball)
        assertTrue(File(root, ".kubriko/actors/Ball.json").isFile)
        assertEquals(ActorTypeCodeGenerator.generateActorType(ball, "com.example.game"), File(generated, "Ball.kt").readText())
        assertContains(File(generated, "GeneratedActorTypes.kt").readText(), "Ball.metadata,")
    }

    @Test
    fun savedDefinitionsLoadBackSortedByName() {
        optIn()
        val store = ActorTypeStore.open(root)!!
        val wall = ActorTypeDefinition(name = "Wall", shape = ActorShape.RECTANGLE, color = 0xFF000000)
        store.save(wall)
        store.save(ball)
        assertEquals(listOf(ball, wall), store.load())
    }

    @Test
    fun savingAgainOverwritesTheDefinitionAndItsClass() {
        optIn()
        val store = ActorTypeStore.open(root)!!
        store.save(ball)
        store.save(ball.copy(shape = ActorShape.RECTANGLE))
        assertEquals(listOf(ball.copy(shape = ActorShape.RECTANGLE)), store.load())
        assertContains(File(generated, "Ball.kt").readText(), "drawRect(")
    }

    @Test
    fun deletingRemovesTheDefinitionAndItsClassAndUpdatesTheRegistry() {
        optIn()
        val store = ActorTypeStore.open(root)!!
        store.save(ball)
        store.delete("Ball")
        assertTrue(store.load().isEmpty())
        assertFalse(File(generated, "Ball.kt").exists())
        assertContains(File(generated, "GeneratedActorTypes.kt").readText(), "emptyList()")
    }

    @Test
    fun regeneratingRestoresEveryGeneratedFile() {
        optIn()
        val store = ActorTypeStore.open(root)!!
        store.save(ball)
        generated.deleteRecursively()
        store.regenerateAll()
        assertTrue(File(generated, "Ball.kt").isFile)
        assertTrue(File(generated, "GeneratedActorTypes.kt").isFile)
    }

    @Test
    fun regeneratingAProjectWithoutActorTypesStillWritesAnEmptyRegistry() {
        optIn()
        ActorTypeStore.open(root)!!.regenerateAll()
        assertContains(File(generated, "GeneratedActorTypes.kt").readText(), "emptyList()")
    }

    @Test
    fun theSourceFolderComesFromTheSettings() {
        optIn("""{ "packageName": "io.game", "sourceDirectory": "shared/src/commonMain/kotlin" }""")
        ActorTypeStore.open(root)!!.save(ball)
        assertTrue(File(root, "shared/src/commonMain/kotlin/io/game/generated/Ball.kt").isFile)
    }

    @Test
    fun definitionsAreStoredAsReadableJsonThatSurvivesUnknownFields() {
        optIn()
        val store = ActorTypeStore.open(root)!!
        store.save(ball)
        val file = File(root, ".kubriko/actors/Ball.json")
        val text = file.readText()
        assertContains(text, "\n    \"name\": \"Ball\",")
        assertContains(text, "\"type\": \"bounceOffEdge\"")
        file.writeText(text.replace("\"name\": \"Ball\",", "\"name\": \"Ball\", \"addedLater\": true,"))
        assertEquals(listOf(ball), store.load())
    }
}

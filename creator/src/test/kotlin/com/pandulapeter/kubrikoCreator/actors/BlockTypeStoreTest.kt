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

class BlockTypeStoreTest {
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
        assertNull(BlockTypeStore.open(root))
    }

    @Test
    fun savingWritesTheDefinitionItsClassAndTheRegistry() {
        optIn()
        val store = assertNotNull(BlockTypeStore.open(root))
        store.save(ball)
        assertTrue(File(root, ".kubriko/actors/Ball.json").isFile)
        assertEquals(ActorTypeCodeGenerator.generateActorType(ball, "com.example.game"), File(generated, "Ball.kt").readText())
        assertContains(File(generated, "GeneratedActorTypes.kt").readText(), "Ball.metadata,")
    }

    @Test
    fun savedDefinitionsLoadBackSortedByName() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        val wall = ActorTypeDefinition(name = "Wall", shape = ActorShape.RECTANGLE, color = 0xFF000000)
        store.save(wall)
        store.save(ball)
        assertEquals(listOf(ball, wall), store.load())
    }

    @Test
    fun savingAgainOverwritesTheDefinitionAndItsClass() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball)
        store.save(ball.copy(shape = ActorShape.RECTANGLE))
        assertEquals(listOf(ball.copy(shape = ActorShape.RECTANGLE)), store.load())
        assertContains(File(generated, "Ball.kt").readText(), "drawRect(")
    }

    @Test
    fun deletingRemovesTheDefinitionAndItsClassAndUpdatesTheRegistry() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball)
        store.delete("Ball")
        assertTrue(store.load().isEmpty())
        assertFalse(File(generated, "Ball.kt").exists())
        assertContains(File(generated, "GeneratedActorTypes.kt").readText(), "emptyList()")
    }

    @Test
    fun regeneratingRestoresEveryGeneratedFile() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball)
        generated.deleteRecursively()
        store.regenerateAll()
        assertTrue(File(generated, "Ball.kt").isFile)
        assertTrue(File(generated, "GeneratedActorTypes.kt").isFile)
    }

    @Test
    fun regeneratingAProjectWithoutActorTypesStillWritesAnEmptyRegistry() {
        optIn()
        BlockTypeStore.open(root)!!.regenerateAll()
        assertContains(File(generated, "GeneratedActorTypes.kt").readText(), "emptyList()")
    }

    @Test
    fun theGeneratedPathIsRelativeToTheProject() {
        optIn()
        assertEquals("src/commonMain/kotlin/com/example/game/generated", BlockTypeStore.open(root)!!.generatedPath)
    }

    @Test
    fun theSourceFolderComesFromTheSettings() {
        optIn("""{ "packageName": "io.game", "sourceDirectory": "shared/src/commonMain/kotlin" }""")
        BlockTypeStore.open(root)!!.save(ball)
        assertTrue(File(root, "shared/src/commonMain/kotlin/io/game/generated/Ball.kt").isFile)
    }

    @Test
    fun definitionsAreStoredAsReadableJsonThatSurvivesUnknownFields() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball)
        val file = File(root, ".kubriko/actors/Ball.json")
        val text = file.readText()
        assertContains(text, "\n    \"name\": \"Ball\",")
        assertContains(text, "\"type\": \"bounceOffEdge\"")
        assertContains(text, "\"color\": \"#6060AA\"")
        assertFalse("null" in text, text)
        file.writeText(text.replace("\"name\": \"Ball\",", "\"name\": \"Ball\", \"addedLater\": true,"))
        assertEquals(listOf(ball), store.load())
    }

    @Test
    fun collisionEventsCompileOnlyWhileTheirOtherActorTypeExists() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ActorTypeDefinition(name = "Paddle"))
        store.save(ball.copy(events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Paddle", blocks = listOf(BlockDefinition.BounceOffOther)))))
        assertContains(File(generated, "Ball.kt").readText(), "is Paddle ->")
        store.delete("Paddle")
        assertFalse("Paddle" in File(generated, "Ball.kt").readText())
        store.save(ActorTypeDefinition(name = "Paddle"))
        assertContains(File(generated, "Ball.kt").readText(), "is Paddle ->")
    }

    @Test
    fun generatedCodeChangedByHandIsKeptAndReported() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        assertEquals(emptySet(), store.save(ball))
        val file = File(generated, "Ball.kt")
        val editedByHand = file.readText().replace("override val body = state.body", "override val body = state.body // tuned")
        file.writeText(editedByHand)
        assertEquals(setOf("Ball"), store.save(ball.copy(shape = ActorShape.RECTANGLE)))
        assertEquals(editedByHand, file.readText())
        assertEquals(setOf("Ball"), store.regenerateAll())
    }

    @Test
    fun changesMadeByHandCanBeDiscarded() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball)
        val file = File(generated, "Ball.kt")
        file.appendText("// tuned\n")
        assertEquals(emptySet(), store.regenerateAll(overwriting = setOf("Ball")))
        assertEquals(ActorTypeCodeGenerator.generateActorType(ball, "com.example.game"), file.readText())
    }

    @Test
    fun aRegistryChangedByHandIsKept() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball)
        val registry = File(generated, "GeneratedActorTypes.kt")
        registry.appendText("// mine\n")
        store.save(ActorTypeDefinition(name = "Wall"))
        assertContains(registry.readText(), "// mine")
    }

    @Test
    fun codeGeneratedBeforeChecksumsIsStillUpdated() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball)
        val file = File(generated, "Ball.kt")
        file.writeText("// Generated by Kubriko Creator from .kubriko/actors/Ball.json. Edit it in Kubriko Creator.\npackage old\n")
        assertEquals(emptySet(), store.regenerateAll())
        assertContains(file.readText(), "package com.example.game.generated")
    }

    @Test
    fun managerTypesAreKeptApartAndWiredIn() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        val game = ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Number("score")), texts = listOf(ScreenTextDefinition("{score}")))
        store.save(ball)
        store.save(game)
        assertTrue(File(root, ".kubriko/managers/Game.json").isFile)
        assertEquals(listOf(ball, game), store.load())
        assertEquals(ManagerTypeCodeGenerator.generateManagerType(game, "com.example.game", listOf("Ball"), listOf(game)), File(generated, "Game.kt").readText())
        assertContains(File(generated, "KubrikoWiring.kt").readText(), "        Game(isInSceneEditor),\n")
        assertEquals(ScenesCodeGenerator.generate("com.example.game"), File(generated, "GeneratedScenes.kt").readText())
        store.delete("Game")
        assertFalse(File(root, ".kubriko/managers/Game.json").exists())
        assertFalse(File(generated, "Game.kt").exists())
        assertContains(File(generated, "KubrikoWiring.kt").readText(), "        GeneratedScenes(firstScene, isInSceneEditor),\n    )\n")
    }

    @Test
    fun actorTypesAreRegeneratedWhenAManagerTheyUseChanges() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        val brick = ActorTypeDefinition("Brick", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeNumber("score", manager = "Game")))))
        store.save(brick)
        assertContains(File(generated, "Brick.kt").readText(), "// Choose a Property for this Block.")
        store.save(ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Number("score"))))
        assertContains(File(generated, "Brick.kt").readText(), "game.score += 1f")
    }

    @Test
    fun aWiringChangedByHandIsLeftAlone() {
        optIn()
        val store = BlockTypeStore.open(root)!!
        store.save(ball)
        File(generated, "KubrikoWiring.kt").appendText("// Mine\n")
        store.save(ManagerTypeDefinition("Game"))
        assertContains(File(generated, "KubrikoWiring.kt").readText(), "// Mine")
    }
}

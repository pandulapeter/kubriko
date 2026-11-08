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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyNaming
import com.pandulapeter.kubrikoCreator.project.ProjectFormat
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LookPropertyMigrationTest {
    private val root = testDirectory()
    private val settingsFile = File(root, ".kubriko/project.json").apply {
        parentFile.mkdirs()
        writeText("""{ "packageName": "com.example.game", "formatVersion": 2 }""")
    }
    private val sceneFile = File(root, "src/commonMain/composeResources/files/scenes/level.json")

    private fun everyFrame(vararg blocks: BlockDefinition) = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = blocks.toList()))

    private fun migratedTypes(): Map<String, Any> {
        assertEquals(ProjectFormatStatus.Writable, ProjectFormat(root, creatorVersion = "1.0.0").prepare())
        val loaded = assertNotNull(BlockTypeStore.open(root)).load()
        assertEquals(emptyList(), loaded.unreadableFiles)
        assertTrue("\"formatVersion\": ${ProjectFormat.CURRENT_VERSION}" in settingsFile.readText())
        return loaded.types.associateBy { it.name }
    }

    @Test
    fun lookCanNoLongerBeAPropertysName() {
        assertEquals(NameProblem.PROPERTY_RESERVED, PropertyNaming.validate("look", emptyList()))
    }

    @Test
    fun anActorFamilysLookIsRenamedWhereItIsDeclaredUsedAndKeptInScenes() {
        val store = assertNotNull(BlockTypeStore.open(root))
        store.save(
            ActorTypeDefinition(
                name = "Enemy",
                properties = listOf(PropertyDefinition.Number("look", 2f), PropertyDefinition.Number("look2")),
                events = everyFrame(BlockDefinition.ChangeNumber(property = "look"), BlockDefinition.Kotlin("look += look2")),
            ),
        )
        store.save(ActorTypeDefinition(name = "Ghost", parent = "Enemy", events = everyFrame(BlockDefinition.Kotlin("look = 0f"))))
        store.save(ActorTypeDefinition(name = "Bat", parent = "Enemy", properties = listOf(PropertyDefinition.Number("look", 5f))))
        store.save(ActorTypeDefinition(name = "Ball", events = everyFrame(BlockDefinition.Kotlin("val look = 1f"))))
        sceneFile.apply { parentFile.mkdirs() }.writeText(
            SceneDocument.format(
                listOf(
                    SceneActorEntry(typeId = "Ghost", state = """{"look":3.0,"look2":1.0}""", id = "a"),
                    SceneActorEntry(typeId = "Ball", state = """{"look":4.0}""", id = "b"),
                ),
            ),
        )
        val types = migratedTypes()
        val enemy = types.getValue("Enemy") as ActorTypeDefinition
        assertEquals(listOf(PropertyDefinition.Number("look3", 2f), PropertyDefinition.Number("look2")), enemy.properties)
        assertEquals(everyFrame(BlockDefinition.ChangeNumber(property = "look3"), BlockDefinition.Kotlin("look3 += look2")), enemy.events)
        assertEquals(everyFrame(BlockDefinition.Kotlin("look3 = 0f")), (types.getValue("Ghost") as ActorTypeDefinition).events)
        assertEquals(listOf(PropertyDefinition.Number("look3", 5f)), (types.getValue("Bat") as ActorTypeDefinition).properties)
        assertEquals(everyFrame(BlockDefinition.Kotlin("val look = 1f")), (types.getValue("Ball") as ActorTypeDefinition).events)
        assertEquals(
            listOf("""{"look3":3.0,"look2":1.0}""", """{"look":4.0}"""),
            SceneDocument.parse(sceneFile.readText()).map { it.state },
        )
    }

    @Test
    fun aManagerTypesLookIsRenamedInEveryTypeThatUsesIt() {
        val store = assertNotNull(BlockTypeStore.open(root))
        store.save(ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Number("look"))))
        store.save(
            ActorTypeDefinition(
                name = "Ball",
                events = everyFrame(BlockDefinition.SetNumber(manager = "Game", property = "look", value = 1f), BlockDefinition.Kotlin("gameManager.look += 1f")),
            ),
        )
        val types = migratedTypes()
        assertEquals(listOf(PropertyDefinition.Number("look2")), (types.getValue("Game") as ManagerTypeDefinition).properties)
        assertEquals(
            everyFrame(BlockDefinition.SetNumber(manager = "Game", property = "look2", value = 1f), BlockDefinition.Kotlin("gameManager.look2 += 1f")),
            (types.getValue("Ball") as ActorTypeDefinition).events,
        )
    }

    @Test
    fun aProjectWithoutALookPropertyIsLeftAsItWas() {
        val store = assertNotNull(BlockTypeStore.open(root))
        store.save(ActorTypeDefinition(name = "Ball", properties = listOf(PropertyDefinition.Number("speed"))))
        val file = File(root, ".kubriko/actors/Ball.json")
        val text = file.readText()
        migratedTypes()
        assertEquals(text, file.readText())
    }
}

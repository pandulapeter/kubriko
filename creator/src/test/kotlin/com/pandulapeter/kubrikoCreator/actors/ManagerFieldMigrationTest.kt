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

import com.pandulapeter.kubrikoCreator.project.ProjectFormat
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManagerFieldMigrationTest {
    private val root = createTempDirectory().toFile()
    private val settingsFile = File(root, ".kubriko/project.json").apply {
        parentFile.mkdirs()
        writeText("""{ "packageName": "com.example.game" }""")
    }
    private val game = ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Number("score")))
    private val ball = ActorTypeDefinition(
        name = "Ball",
        folder = "things",
        events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin("game.score += 1f")))),
    )

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun anUnversionedProjectsKotlinCodeNamesTheManagerFieldsAsTheyAreNow() {
        val store = BlockTypeStore.open(root)!!
        store.save(game)
        store.save(ball)
        val gameFile = File(root, ".kubriko/managers/Game.json")
        val gameText = gameFile.readText()
        File(root, ".kubriko/actors/things/Broken.json").writeText("{ not json")
        assertEquals(ProjectFormatStatus.Writable, ProjectFormat(root, creatorVersion = "1.0.0").prepare())
        val loaded = BlockTypeStore.open(root)!!.load()
        assertEquals(
            BlockDefinition.Kotlin("gameManager.score += 1f"),
            (loaded.types.single { it.name == "Ball" } as ActorTypeDefinition).events.single().blocks.single(),
        )
        assertEquals("things", loaded.types.single { it.name == "Ball" }.folder)
        assertEquals(gameText, gameFile.readText())
        assertEquals("{ not json", File(root, ".kubriko/actors/things/Broken.json").readText())
        assertTrue("\"formatVersion\": 1" in settingsFile.readText())
    }

    @Test
    fun aProjectOfTheCurrentVersionIsLeftAlone() {
        settingsFile.writeText("""{ "packageName": "com.example.game", "formatVersion": ${ProjectFormat.CURRENT_VERSION} }""")
        BlockTypeStore.open(root)!!.save(ball)
        ProjectFormat(root, creatorVersion = "1.0.0").prepare()
        assertEquals(BlockDefinition.Kotlin("game.score += 1f"), (BlockTypeStore.open(root)!!.load().types.single() as ActorTypeDefinition).events.single().blocks.single())
    }
}

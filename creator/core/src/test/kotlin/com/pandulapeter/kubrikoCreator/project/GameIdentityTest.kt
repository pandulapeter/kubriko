/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(EditorMutation::class)

package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameIdentityTest {
    private val root = createTempDirectory().toFile()
    private val identity = GameIdentity("My Game", "1.0.0", 1, "com.example.mygame", "", "")

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun whatInstallersAndStoresRefuseIsRejected() {
        assertNull(identity.validate())
        assertNull(identity.copy(versionName = "2", versionNumber = 2_100_000_000, applicationId = "io.github.Me_2.game").validate())
        assertEquals(IdentityProblem.NAME_EMPTY, identity.copy(name = " ").validate()?.first)
        assertNull(identity.copy(name = "Kalandjáték").validate())
        assertEquals(IdentityProblem.NAME_INVALID, identity.copy(name = "a\nb").validate()?.first)
        listOf("0.1", "1.2.3.4", "1.", "v1", "").forEach { versionName ->
            assertEquals(IdentityProblem.VERSION_NAME_INVALID to listOf(versionName), identity.copy(versionName = versionName).validate(), versionName)
        }
        assertEquals(IdentityProblem.VERSION_NUMBER_INVALID, identity.copy(versionNumber = 0).validate()?.first)
        mapOf(
            "game" to IdentityProblem.APPLICATION_ID_PARTS,
            "com.1game" to IdentityProblem.APPLICATION_ID_FIRST_CHARACTER,
            "com..game" to IdentityProblem.APPLICATION_ID_PART_EMPTY,
            "com.my-game" to IdentityProblem.APPLICATION_ID_CHARACTERS,
            "com.example.kalandjáték" to IdentityProblem.APPLICATION_ID_LETTER_BEYOND_A_TO_Z,
        ).forEach { (applicationId, problem) ->
            assertEquals(problem to listOf(applicationId), identity.copy(applicationId = applicationId).validate(), applicationId)
        }
        assertEquals(IdentityProblem.TEXT_INVALID, identity.copy(vendor = "x".repeat(201)).validate()?.first)
        assertEquals(IdentityProblem.TEXT_INVALID, identity.copy(description = "a\rb").validate()?.first)
    }

    @Test
    fun anIdentityIsReadOnlyWhereTheBuildReadsIt() {
        GameIdentity.write(root, identity.copy(vendor = "Péter"))
        assertNull(GameIdentity.read(root), "no build file")
        File(root, "build.gradle.kts").writeText("val gameName = providers.gradleProperty(\"game.name\")\n")
        assertEquals(identity.copy(vendor = "Péter"), GameIdentity.read(root))
        File(root, "gradle.properties").writeText("game.name=Only the name\n")
        assertNull(GameIdentity.read(root), "keys missing")
    }

    @Test
    fun aPropertiesFileWithWindowsLineBreaksKeepsThem() {
        val properties = File(root, "gradle.properties").apply { writeText("kotlin.code.style=official\r\ngame.name=Old\r\n") }
        GameIdentity.write(root, identity)
        assertEquals(identity.writtenTo("kotlin.code.style=official\ngame.name=Old\n").replace("\n", "\r\n"), properties.readText())
    }

    @Test
    fun writingChangesOnlyTheIdentitysLines() {
        val before = "kotlin.code.style=official\ngame.name=Old\n# a comment\ngame.versionNumber=7\n"
        assertEquals(
            "kotlin.code.style=official\ngame.name=My Game\n# a comment\ngame.versionNumber=1\n" +
                "game.versionName=1.0.0\ngame.applicationId=com.example.mygame\ngame.vendor=\ngame.description=\n",
            identity.writtenTo(before),
        )
        assertEquals(identity, GameIdentity.of(identity.writtenTo(before)))
        assertNull(GameIdentity.of(identity.writtenTo("").replace("game.versionNumber=1", "game.versionNumber=one")))
    }
}

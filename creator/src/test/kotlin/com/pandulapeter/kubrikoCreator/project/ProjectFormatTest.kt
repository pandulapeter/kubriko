/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProjectFormatTest {
    private val rootDirectory = createTempDirectory().toFile()
    private val settingsFile = File(rootDirectory, ".kubriko/project.json").apply { parentFile.mkdirs() }
    private val applied = mutableListOf<String>()

    private fun migration(name: String) = FormatMigration { root ->
        applied += name
        File(root, "$name.txt").writeText(name)
    }

    private fun format(vararg migrations: FormatMigration) = ProjectFormat(rootDirectory, creatorVersion = "1.2.0", migrations = migrations.toList())

    private fun settings() = Json.parseToJsonElement(settingsFile.readText()).jsonObject

    @AfterTest
    fun tearDown() {
        rootDirectory.deleteRecursively()
    }

    @Test
    fun aProjectThatHasNotOptedInIsWritable() {
        settingsFile.delete()
        assertEquals(ProjectFormatStatus.Writable, format(migration("first")).prepare())
        assertEquals(emptyList(), applied)
    }

    @Test
    fun anUnversionedProjectIsVersionZeroAndGetsEveryMigrationInOrder() {
        settingsFile.writeText("""{ "packageName": "com.example.game", "templateVersion": 7 }""")
        assertEquals(ProjectFormatStatus.Writable, format(migration("first"), migration("second")).prepare())
        assertEquals(listOf("first", "second"), applied)
        assertEquals(2, settings().getValue("formatVersion").jsonPrimitive.int)
        assertEquals("1.2.0", settings().getValue("minimumCreatorVersion").jsonPrimitive.content)
        assertEquals("com.example.game", settings().getValue("packageName").jsonPrimitive.content)
        assertEquals(7, settings().getValue("templateVersion").jsonPrimitive.int)
    }

    @Test
    fun onlyTheMigrationsAfterTheRecordedVersionAreApplied() {
        settingsFile.writeText("""{ "packageName": "com.example.game", "formatVersion": 1 }""")
        format(migration("first"), migration("second")).prepare()
        assertEquals(listOf("second"), applied)
    }

    @Test
    fun aProjectAtTheCurrentVersionIsLeftAlone() {
        val text = """{ "packageName": "com.example.game", "formatVersion": 1 }"""
        settingsFile.writeText(text)
        assertEquals(ProjectFormatStatus.Writable, format(migration("first")).prepare())
        assertEquals(emptyList(), applied)
        assertEquals(text, settingsFile.readText())
    }

    @Test
    fun aNewerProjectIsReadOnlyAndNamesTheCreatorThatReadsIt() {
        val text = """{ "packageName": "com.example.game", "formatVersion": 3, "minimumCreatorVersion": "2.0.0" }"""
        settingsFile.writeText(text)
        assertEquals(ProjectFormatStatus.ReadOnly(3, "2.0.0"), format(migration("first")).prepare())
        assertEquals(text, settingsFile.readText())
        settingsFile.writeText("""{ "packageName": "com.example.game", "formatVersion": 1 }""")
        assertEquals(ProjectFormatStatus.ReadOnly(1, null), format().prepare())
    }

    @Test
    fun aMigrationThatFailsKeepsTheVersionsBeforeItSoTheNextTryCarriesOn() {
        settingsFile.writeText("""{ "packageName": "com.example.game" }""")
        val failing = FormatMigration { throw IOException("Disk full.") }
        assertFailsWith<IOException> { format(migration("first"), failing).prepare() }
        assertEquals(1, settings().getValue("formatVersion").jsonPrimitive.int)
        format(migration("first"), migration("second")).prepare()
        assertEquals(listOf("first", "second"), applied)
    }

    @Test
    fun anUnreadableSettingsFileIsLeftToTheTypeStoreToReport() {
        settingsFile.writeText("{ not json")
        assertEquals(ProjectFormatStatus.Writable, format(migration("first")).prepare())
        assertEquals(emptyList(), applied)
    }
}

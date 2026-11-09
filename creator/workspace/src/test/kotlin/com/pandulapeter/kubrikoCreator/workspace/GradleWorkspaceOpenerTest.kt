/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.gradle.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeFalse
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GradleWorkspaceOpenerTest {
    private val opener = GradleWorkspaceOpener(CreatorConfiguration.load(), loadCreatorLibraries = { emptyMap() })

    @Test
    fun aFolderThatIsNotAGradleProjectIsRefused() = runTest {
        val exception = assertFailsWith<ProjectInspectionException> { opener.open(testDirectory(), backgroundScope) }
        assertEquals(ProjectProblem.NOT_A_GRADLE_PROJECT, exception.reason)
    }

    @Test
    fun aProjectWhoseFormatCantBeUpgradedIsRefused() = runTest {
        val directory = testDirectory()
        File(directory, "settings.gradle.kts").writeText("rootProject.name = \"game\"\n")
        val kubriko = File(directory, ".kubriko").apply { mkdirs() }
        File(kubriko, "project.json").writeText("""{ "packageName": "com.example.game" }""")
        kubriko.setWritable(false)
        try {
            assumeFalse("the folder still takes new files", runCatching { File(kubriko, "probe").createNewFile() }.getOrDefault(false))
            val exception = assertFailsWith<ProjectInspectionException> { opener.open(directory, backgroundScope) }
            assertEquals(ProjectProblem.FORMAT_NOT_UPGRADED_ON_OPEN, exception.reason)
        } finally {
            kubriko.setWritable(true)
        }
    }

    @Test
    fun aProjectWhoseProjectFileIsInvalidIsRefusedNamingTheFileAndTheMistake() = runTest {
        val directory = testDirectory()
        File(directory, "settings.gradle.kts").writeText("rootProject.name = \"game\"\n")
        val settingsFile = File(directory, ".kubriko/project.json").apply { parentFile.mkdirs() }
        settingsFile.writeText("{\n  \"packageName\": \"com.example.game\",\n}\n")
        val exception = assertFailsWith<ProjectInspectionException> { opener.open(directory, backgroundScope) }
        assertEquals(ProjectProblem.PROJECT_FILE_NOT_JSON, exception.reason)
        assertEquals(listOf(settingsFile.path, "2"), exception.arguments)
    }

    @Test
    fun aProjectFileThatAnOlderFormatsMigrationCantReadIsNamedToo() = runTest {
        val directory = testDirectory()
        File(directory, "settings.gradle.kts").writeText("rootProject.name = \"game\"\n")
        val settingsFile = File(directory, ".kubriko/project.json").apply { parentFile.mkdirs() }
        settingsFile.writeText("""{ "formatVersion": 0 }""")
        val exception = assertFailsWith<ProjectInspectionException> { opener.open(directory, backgroundScope) }
        assertEquals(ProjectProblem.PROJECT_FILE_VALUE_MISSING, exception.reason)
        assertEquals(listOf(settingsFile.path, "packageName"), exception.arguments)
    }

    @Test
    fun aNewerProjectWhoseSettingsThisCreatorCantReadIsBrowsedWithoutItsTypes() {
        val directory = testDirectory()
        File(directory, ".kubriko/project.json").apply { parentFile.mkdirs() }.writeText("""{ "packageName": { "root": "com.example.game" }, "formatVersion": 99 }""")
        assertNull(GradleWorkspaceOpener.openBlockTypeStore(directory, OwnWrites(), ProjectFormatStatus.ReadOnly(99, "9.0.0")))
        val exception = assertFailsWith<ProjectInspectionException> { GradleWorkspaceOpener.openBlockTypeStore(directory, OwnWrites(), ProjectFormatStatus.Writable) }
        assertEquals(ProjectProblem.PROJECT_FILE_VALUE_NOT_TEXT, exception.reason)
    }
}

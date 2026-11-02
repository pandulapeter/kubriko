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

import com.pandulapeter.kubrikoCreator.project.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_not_a_gradle_project
import kubriko.creator.generated.resources.error_project_format_not_upgraded_on_open
import org.junit.Assume.assumeFalse
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GradleWorkspaceOpenerTest {

    @Test
    fun aFolderThatIsNotAGradleProjectIsRefused() = runTest {
        val exception = assertFailsWith<ProjectInspectionException> { GradleWorkspaceOpener.open(testDirectory(), backgroundScope) }
        assertEquals(Res.string.error_not_a_gradle_project, exception.reason)
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
            val exception = assertFailsWith<ProjectInspectionException> { GradleWorkspaceOpener.open(directory, backgroundScope) }
            assertEquals(Res.string.error_project_format_not_upgraded_on_open, exception.reason)
        } finally {
            kubriko.setWritable(true)
        }
    }
}

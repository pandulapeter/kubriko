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

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectInspectorTest {

    @Test
    fun aFolderIsAGradleProjectWhenItHasABuildOrSettingsFile() = withTemporaryDirectory { directory ->
        assertFalse(ProjectInspector.isGradleProject(directory))
        File(directory, "build.gradle.kts").writeText("")
        assertTrue(ProjectInspector.isGradleProject(directory))
    }

    @Test
    fun theNameIsTheOneTheSettingsGiveTheRootProjectOrTheFolders() = withTemporaryDirectory { directory ->
        assertEquals(directory.name, ProjectInspector.nameOf(directory))
        File(directory, "settings.gradle.kts").writeText("pluginManagement {}\n\nrootProject.name = \"breakout\"\n")
        assertEquals("breakout", ProjectInspector.nameOf(directory))
    }

    private fun withTemporaryDirectory(test: (File) -> Unit) {
        val directory = createTempDirectory().toFile()
        try {
            test(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}

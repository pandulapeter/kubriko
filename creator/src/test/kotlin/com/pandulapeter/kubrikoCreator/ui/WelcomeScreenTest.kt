/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import java.io.File
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class WelcomeScreenTest {

    @Test
    fun anUnreadableProjectFileIsNamedWithWhatIsWrongInIt() = uiTest(width = 900, height = 600) {
        val path = File("/Users/someone/Games/My Game/.kubriko/project.json").path
        showing {
            WelcomeScreen(
                error = ProjectProblem.PROJECT_FILE_NOT_JSON.resource,
                errorArguments = listOf(path, "3"),
                recentProjects = emptyList(),
                onNewProject = {},
                onOpenProject = {},
                onForgetRecentProject = {},
                onOpenSettings = {},
            )
        }
        onNodeWithText("$path isn't valid JSON: something on line 3 is out of place. Fix it in a text editor, or bring it back from version control.").assertIsDisplayed()
        saveSnapshot("welcome-unreadable-project-file")
    }
}

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.gradle

import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DesktopModulesTest {

    @Test
    fun aSingleModuleProjectRunsFromTheRoot() {
        assertEquals(":", DesktopModules.find(ModuleNode(":", setOf("build", "run"))))
    }

    @Test
    fun theFirstNestedModuleWithARunTaskIsTheDesktopModule() {
        val root = ModuleNode(
            path = ":",
            taskNames = setOf("build"),
            children = listOf(
                ModuleNode(":app", setOf("build"), listOf(ModuleNode(":app:android", setOf("assemble")), ModuleNode(":app:desktop", setOf("run")))),
                ModuleNode(":tools", setOf("run")),
            ),
        )
        assertEquals(":app:desktop", DesktopModules.find(root))
    }

    @Test
    fun aProjectWithoutADesktopModuleIsRejectedWithAReason() {
        val exception = assertFailsWith<ProjectInspectionException> {
            DesktopModules.find(ModuleNode(":", setOf("build"), listOf(ModuleNode(":android", setOf("assemble")))))
        }
        assertEquals(ProjectProblem.NO_DESKTOP_MODULE, exception.reason)
    }
}

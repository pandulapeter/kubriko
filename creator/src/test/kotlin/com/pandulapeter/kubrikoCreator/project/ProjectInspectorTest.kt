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

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_no_desktop_module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProjectInspectorTest {

    @Test
    fun aSingleModuleProjectRunsFromTheRoot() {
        assertEquals(":", ProjectInspector.findDesktopModulePath(ModuleNode(":", setOf("build", "run"))))
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
        assertEquals(":app:desktop", ProjectInspector.findDesktopModulePath(root))
    }

    @Test
    fun aProjectWithoutADesktopModuleIsRejectedWithAReason() {
        val exception = assertFailsWith<ProjectInspectionException> {
            ProjectInspector.findDesktopModulePath(ModuleNode(":", setOf("build"), listOf(ModuleNode(":android", setOf("assemble")))))
        }
        assertEquals(Res.string.error_no_desktop_module, exception.reason)
    }
}

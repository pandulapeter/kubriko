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

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopModulesTest {

    @Test
    fun aSingleModuleProjectRunsFromTheRoot() {
        assertEquals(listOf(":"), DesktopModules.pathsIn(ModuleNode(":", setOf("build", "run"))))
    }

    @Test
    fun everyModuleWithARunTaskIsListedInGradlesOrder() {
        val root = ModuleNode(
            path = ":",
            taskNames = setOf("build"),
            children = listOf(
                ModuleNode(":app", setOf("build"), listOf(ModuleNode(":app:android", setOf("assemble")), ModuleNode(":app:desktop", setOf("run")))),
                ModuleNode(":editor", setOf("build"), listOf(ModuleNode(":editor:model", setOf("run")), ModuleNode(":editor:region", setOf("run")))),
                ModuleNode(":gameplay", setOf("build")),
            ),
        )
        assertEquals(listOf(":app:desktop", ":editor:model", ":editor:region"), DesktopModules.pathsIn(root))
    }

    @Test
    fun aProjectWithoutADesktopModuleHasNone() {
        assertEquals(emptyList(), DesktopModules.pathsIn(ModuleNode(":", setOf("build"), listOf(ModuleNode(":android", setOf("assemble"))))))
    }
}

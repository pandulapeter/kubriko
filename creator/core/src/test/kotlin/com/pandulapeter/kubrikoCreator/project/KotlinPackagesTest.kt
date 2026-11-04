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

class KotlinPackagesTest {

    @Test
    fun thePackageIsReadPastCommentsAndFileAnnotations() {
        assertEquals(
            "com.game.ui",
            KotlinPackages.declaredPackage(
                """
                /*
                 * License
                 */
                // A note
                @file:JvmName("Menus")
                /* one */ package com.game.ui // trailing
                
                import a.b
                """.trimIndent().lineSequence(),
            ),
        )
    }

    @Test
    fun aSemicolonBackticksAndWindowsLineBreaksDoNotEndUpInThePackage() {
        assertEquals("game.in.level", KotlinPackages.declaredPackage(sequenceOf("package game.`in`.level;\r")))
    }

    @Test
    fun aFileWithoutAPackageLineIsInTheDefaultPackage() {
        assertEquals("", KotlinPackages.declaredPackage(sequenceOf("// Nothing", "import a.b", "package not.this")))
        assertEquals("", KotlinPackages.declaredPackage(emptySequence()))
        assertEquals("", KotlinPackages.declaredPackage(sequenceOf("/* never closed", "package not.this")))
    }

    @Test
    fun aFileThatCannotBeReadFallsBackToItsFolders() {
        val root = createTempDirectory().toFile()
        try {
            assertEquals("game.ui", KotlinPackages.of(File(root, "missing.kt"), "app/src/commonMain/kotlin/game/ui/Menu.kt"))
            assertEquals("", KotlinPackages.packageOfFolders("src/commonMain/kotlin/Main.kt"))
            assertEquals("", KotlinPackages.packageOfFolders("scripts/Tool.kt"))
        } finally {
            root.deleteRecursively()
        }
    }
}

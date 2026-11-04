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

import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectModulesTest {
    private val root = testDirectory()

    private fun write(path: String, text: String = "") = File(root, path).apply { parentFile.mkdirs() }.writeText(text)

    @Test
    fun everyFolderWithABuildFileIsAModuleThatUsesKubrikoWhenItNamesOneOfItsLibraries() {
        write(VersionCatalog.FILE, "[libraries]\nkubriko-engine = { module = \"io.github.pandulapeter.kubriko:engine\", version.ref = \"kubriko\" }\n\n[bundles]\nkubriko = [\"kubriko-engine\"]\n")
        write("settings.gradle.kts", "include(\":game\", \":app:desktop\", \":tools\", \":lookalike\")\n")
        write("build.gradle.kts", "plugins { alias(libs.plugins.kotlinMultiplatform) apply false }\n")
        write("game/build.gradle.kts", "dependencies {\n    implementation(libs.kubriko.engine)\n}\n")
        write("app/desktop/build.gradle", "dependencies { implementation project(':game') }\n")
        write("tools/build.gradle.kts", "dependencies { implementation(\"io.github.pandulapeter.kubriko:plugin-physics:0.1.0\") }\n")
        write("lookalike/build.gradle.kts", "dependencies { implementation(libs.kubriko.engineTools); implementation(otherlibs.kubriko.engine) }\n")
        write("commented/build.gradle.kts", "dependencies {\n    // implementation(libs.kubriko.engine)\n    /* implementation(libs.kubriko.engine) */\n}\n")
        write("bundled/build.gradle.kts", "dependencies { implementation(libs.bundles.kubriko) }\n")
        write("game/src/main/build.gradle.kts", "Not a module: build files in sources aren't read.")
        write("game/build/build.gradle.kts", "Build output.")
        assertEquals(
            mapOf("" to false, "game" to true, "app/desktop" to false, "tools" to true, "lookalike" to false, "commented" to false, "bundled" to true),
            ProjectModules.find(root).mapValues { it.value.usesKubriko },
        )
    }

    @Test
    fun aProjectWithoutACatalogStillKnowsKubrikoByItsCoordinates() {
        write("build.gradle.kts", "dependencies { implementation(\"io.github.pandulapeter.kubriko:engine:0.1.0\") }\n")
        assertEquals(mapOf("" to ProjectModule(usesKubriko = true)), ProjectModules.find(root))
    }

    @Test
    fun aBuildOfItsOwnIsToldApartAndASnapshotsBuildFilesAreReadWithoutAWalk() {
        write("build.gradle.kts")
        write("buildSrc/build.gradle.kts")
        write("gradle/build-logic/settings.gradle.kts")
        write("gradle/build-logic/build.gradle.kts")
        write("game/build.gradle.kts", "implementation(libs.kubriko.engine)")
        assertEquals(
            mapOf("" to false, "buildSrc" to true, "gradle/build-logic" to true, "game" to false),
            ProjectModules.find(root).mapValues { it.value.isBuildRoot },
        )
        val snapshot = ProjectSnapshot(mapOf("game/build.gradle.kts" to FileStamp(1, 1), "gone/build.gradle.kts" to FileStamp(1, 1)))
        assertEquals(setOf("game"), ProjectModules.find(root, snapshot).keys, "Only the snapshot's build files are read, and one gone since is skipped")
    }
}

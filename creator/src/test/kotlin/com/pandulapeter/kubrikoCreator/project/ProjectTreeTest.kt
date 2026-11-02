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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectTreeTest {
    private val root = testDirectory()

    private fun write(vararg paths: String) = paths.forEach { File(root, it).apply { parentFile.mkdirs() }.writeText("") }

    private fun walkedFiles(root: File = this.root, skipped: Set<String> = emptySet(), hiddenDirectory: String? = null) =
        ProjectTree.walk(root, skipped, hiddenDirectory).filter { it.isFile }.map { it.relativeTo(root).invariantSeparatorsPath }.sorted().toList()

    @Test
    fun buildAndToolFoldersAreSkippedOutsideSourceFolders() {
        val skippedFolders = listOf("build", "out", "node_modules", "Pods", "DerivedData", ".gradle", ".git")
        skippedFolders.forEach { write("$it/File.kt", "app/$it/File.kt") }
        write("app/src/commonMain/kotlin/Game.kt")
        assertEquals(listOf("app/src/commonMain/kotlin/Game.kt"), walkedFiles())
    }

    @Test
    fun foldersInsideSourceFoldersMayHaveAnyName() {
        write("src/commonMain/kotlin/com/game/build/Level.kt", "src/commonMain/composeResources/files/scenes/out/level.json", "app/src/main/node_modules/Tool.kt")
        assertEquals(
            listOf("app/src/main/node_modules/Tool.kt", "src/commonMain/composeResources/files/scenes/out/level.json", "src/commonMain/kotlin/com/game/build/Level.kt"),
            walkedFiles(),
        )
    }

    @Test
    fun skippedFoldersAreSkippedInsideSourceFoldersToo() {
        write(".kubriko/project.json", "src/commonMain/kotlin/game/generated/Ball.kt", "src/commonMain/kotlin/game/Game.kt")
        assertEquals(listOf("src/commonMain/kotlin/game/Game.kt"), walkedFiles(skipped = setOf(".kubriko", "generated")))
    }

    @Test
    fun theHiddenFolderIsSkipped() {
        write("src/commonMain/kotlin/game/generated/Ball.kt", "src/commonMain/kotlin/other/generated/Kept.kt")
        assertEquals(listOf("src/commonMain/kotlin/other/generated/Kept.kt"), walkedFiles(hiddenDirectory = "src/commonMain/kotlin/game/generated"))
    }

    @Test
    fun aProjectRootNamedBuildIsWalked() {
        val buildRoot = File(root, "build")
        File(buildRoot, "src/Game.kt").apply { parentFile.mkdirs() }.writeText("")
        assertEquals(listOf("src/Game.kt"), walkedFiles(root = buildRoot))
    }

    @Test
    fun onlyWhatIsBelowASourceFolderIsWithinSources() {
        assertTrue(ProjectTree.isWithinSources(root, File(root, "app/src/main/kotlin/Game.kt")))
        assertFalse(ProjectTree.isWithinSources(root, File(root, "app/src")))
        assertFalse(ProjectTree.isWithinSources(File(root, "src"), File(root, "src/build")))
    }
}

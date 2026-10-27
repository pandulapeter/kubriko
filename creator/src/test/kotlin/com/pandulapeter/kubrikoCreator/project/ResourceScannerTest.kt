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
import kotlin.test.assertNull

class ResourceScannerTest {

    @Test
    fun scenesAreJsonFilesInAScenesFolderOfComposeResources() {
        assertEquals(ResourceGroup.SCENES, ResourceScanner.classify("src/commonMain/composeResources/files/scenes/level.json"))
        assertNull(ResourceScanner.classify("src/commonMain/composeResources/files/models/tree.json"))
        assertNull(ResourceScanner.classify("scenes/level.json"))
    }

    @Test
    fun mediaIsClassifiedByExtensionInsideComposeResourcesOnly() {
        assertEquals(ResourceGroup.SPRITES, ResourceScanner.classify("app/src/commonMain/composeResources/drawable/ball.PNG"))
        assertEquals(ResourceGroup.SOUNDS, ResourceScanner.classify("src/commonMain/composeResources/files/bounce.wav"))
        assertEquals(ResourceGroup.MUSIC, ResourceScanner.classify("src/commonMain/composeResources/files/theme.mp3"))
        assertNull(ResourceScanner.classify("docs/screenshot.png"))
    }

    @Test
    fun kotlinFilesAreTheOnesUnderASourceFolder() {
        assertEquals(ResourceGroup.KOTLIN, ResourceScanner.classify("app/src/commonMain/kotlin/game/Ball.kt"))
        assertNull(ResourceScanner.classify("build.gradle.kts"))
    }

    @Test
    fun scanningGroupsAndSortsFilesAndSkipsBuildAndToolFolders() {
        val root = createTempDirectory().toFile()
        try {
            listOf(
                "src/main/composeResources/files/scenes/b.json",
                "src/main/composeResources/files/scenes/a.json",
                "src/main/kotlin/Game.kt",
                "build/generated/src/main/kotlin/Generated.kt",
                ".gradle/src/Cached.kt",
                "app/build/composeResources/files/scenes/copy.json",
                "README.md",
            ).forEach { File(root, it).apply { parentFile.mkdirs() }.writeText("") }
            val resources = ResourceScanner.scan(root)
            assertEquals(setOf(ResourceGroup.SCENES, ResourceGroup.KOTLIN), resources.keys)
            assertEquals(
                listOf("src/main/composeResources/files/scenes/a.json", "src/main/composeResources/files/scenes/b.json"),
                resources.getValue(ResourceGroup.SCENES).map { it.relativePath },
            )
            assertEquals(listOf(File(root, "src/main/kotlin/Game.kt")), resources.getValue(ResourceGroup.KOTLIN).map { it.file })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun aRootFolderNamedLikeAnIgnoredFolderIsStillScanned() {
        val root = File(createTempDirectory().toFile(), "build")
        try {
            File(root, "src/Game.kt").apply { parentFile.mkdirs() }.writeText("")
            assertEquals(listOf("src/Game.kt"), ResourceScanner.scan(root).getValue(ResourceGroup.KOTLIN).map { it.relativePath })
        } finally {
            root.parentFile.deleteRecursively()
        }
    }

    @Test
    fun theHiddenFolderIsLeftOut() {
        val root = createTempDirectory().toFile()
        try {
            listOf("src/commonMain/kotlin/game/Game.kt", "src/commonMain/kotlin/game/generated/Ball.kt").forEach {
                File(root, it).apply { parentFile.mkdirs() }.writeText("")
            }
            assertEquals(
                listOf("src/commonMain/kotlin/game/Game.kt"),
                ResourceScanner.scan(root, hiddenDirectory = "src/commonMain/kotlin/game/generated").getValue(ResourceGroup.KOTLIN).map { it.relativePath },
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun theSourceSetTellsApartFilesOfTheSameName() {
        fun resource(path: String) = ProjectResource(ResourceGroup.KOTLIN, File(path), path)
        assertEquals("desktopMain", ResourceScanner.sourceSetOf(resource("src/desktopMain/kotlin/game/Main.kt")))
        assertEquals("wasmJsMain", ResourceScanner.sourceSetOf(resource("app/src/wasmJsMain/kotlin/Main.kt")))
        assertNull(ResourceScanner.sourceSetOf(resource("scripts/Tool.kt")))
    }
}

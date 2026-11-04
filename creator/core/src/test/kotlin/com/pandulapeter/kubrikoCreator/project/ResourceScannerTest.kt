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
                "src/main/kotlin/out/Foo.kt",
                "build/generated/src/main/kotlin/Generated.kt",
                ".gradle/src/Cached.kt",
                "Pods/src/main/composeResources/files/scenes/pod.json",
                "app/build/composeResources/files/scenes/copy.json",
                "README.md",
            ).forEach { File(root, it).apply { parentFile.mkdirs() }.writeText("") }
            val resources = ResourceScanner.scan(root)
            assertEquals(setOf(ResourceGroup.SCENES, ResourceGroup.KOTLIN), resources.keys)
            assertEquals(
                listOf("src/main/composeResources/files/scenes/a.json", "src/main/composeResources/files/scenes/b.json"),
                resources.getValue(ResourceGroup.SCENES).map { it.relativePath },
            )
            assertEquals(listOf(File(root, "src/main/kotlin/Game.kt"), File(root, "src/main/kotlin/out/Foo.kt")), resources.getValue(ResourceGroup.KOTLIN).map { it.file })
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
    fun aPackageNamedBuildIsShown() {
        val root = createTempDirectory().toFile()
        try {
            listOf("src/commonMain/kotlin/com/game/build/Level.kt", "src/commonMain/composeResources/files/scenes/out/level.json").forEach {
                File(root, it).apply { parentFile.mkdirs() }.writeText("")
            }
            val resources = ResourceScanner.scan(root)
            assertEquals(listOf("src/commonMain/kotlin/com/game/build/Level.kt"), resources.getValue(ResourceGroup.KOTLIN).map { it.relativePath })
            assertEquals(listOf("src/commonMain/composeResources/files/scenes/out/level.json"), resources.getValue(ResourceGroup.SCENES).map { it.relativePath })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun aScanFromASnapshotEqualsAWalk() {
        val root = createTempDirectory().toFile()
        val hidden = "src/commonMain/kotlin/game/generated"
        try {
            listOf(
                "src/commonMain/kotlin/game/Game.kt",
                "src/commonMain/kotlin/game/build/Level.kt",
                "$hidden/Ball.kt",
                "src/commonMain/composeResources/files/scenes/main.json",
                "src/commonMain/composeResources/files/scenes/main.json.creator-write.tmp",
                "src/commonMain/composeResources/drawable/ball.png",
                "src/commonMain/composeResources/files/theme.mp3",
                "src/commonMain/composeResources/files/bounce.wav",
                "app/composeResources/files/scenes/outside.json",
                "app/composeResources/drawable/outside.png",
                ".kubriko/project.json",
                ".kubriko/actors/Ball.json",
                "src/.kubriko/Kept.kt",
                "build/src/Generated.kt",
                "Pods/src/main/composeResources/drawable/pod.png",
                "README.md",
            ).forEach { File(root, it).apply { parentFile.mkdirs() }.writeText("") }
            val walked = ResourceScanner.scan(root, hiddenDirectory = hidden)
            assertEquals(walked, ResourceScanner.scan(root, hiddenDirectory = hidden, snapshot = ProjectWatcher.snapshot(root, hidden)))
            assertEquals(setOf(ResourceGroup.KOTLIN, ResourceGroup.SCENES, ResourceGroup.SPRITES, ResourceGroup.SOUNDS, ResourceGroup.MUSIC), walked.keys)
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
        assertNull(ResourceScanner.sourceSetOf(resource("scripts/src/Tool.kt")))
    }

    @Test
    fun kotlinFilesCarryThePackageTheyDeclare() {
        val root = createTempDirectory().toFile()
        try {
            File(root, "src/commonMain/kotlin/ui/Menu.kt").apply { parentFile.mkdirs() }.writeText("package com.game.ui\n")
            File(root, "src/commonMain/composeResources/drawable/ball.png").apply { parentFile.mkdirs() }.writeText("")
            val resources = ResourceScanner.scan(root)
            assertEquals(listOf("com.game.ui"), resources.getValue(ResourceGroup.KOTLIN).map { it.kotlinPackage })
            assertEquals(listOf(null), resources.getValue(ResourceGroup.SPRITES).map { it.kotlinPackage })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun everyFileBelongsToTheNearestFolderWithABuildFileWhetherWalkedOrFromASnapshot() {
        val root = createTempDirectory().toFile()
        try {
            listOf(
                "settings.gradle.kts",
                "build.gradle.kts",
                "src/commonMain/kotlin/Game.kt",
                "shared/build.gradle",
                "shared/src/commonMain/kotlin/Shared.kt",
                "shared/src/commonMain/composeResources/drawable/hero.png",
                "app/desktop/build.gradle.kts",
                "app/desktop/src/main/kotlin/Main.kt",
                "app/src/main/kotlin/NoModuleOfItsOwn.kt",
            ).forEach { File(root, it).apply { parentFile.mkdirs() }.writeText("") }
            val walked = ResourceScanner.scan(root)
            assertEquals(
                mapOf(
                    "app/desktop/src/main/kotlin/Main.kt" to "app/desktop",
                    "app/src/main/kotlin/NoModuleOfItsOwn.kt" to "",
                    "shared/src/commonMain/kotlin/Shared.kt" to "shared",
                    "src/commonMain/kotlin/Game.kt" to "",
                ),
                walked.getValue(ResourceGroup.KOTLIN).associate { it.relativePath to it.module },
            )
            assertEquals(listOf("shared"), walked.getValue(ResourceGroup.SPRITES).map { it.module })
            assertEquals(walked, ResourceScanner.scan(root, snapshot = ProjectWatcher.snapshot(root, null)))
            assertEquals("app/desktop", ResourceScanner.moduleOf(root, "app/desktop/src/main/kotlin/game/generated"))
            assertEquals("shared", ResourceScanner.moduleOf(root, "shared"))
            assertEquals("", ResourceScanner.moduleOf(root, "src/commonMain/composeResources/files/scenes"))
        } finally {
            root.deleteRecursively()
        }
    }
}

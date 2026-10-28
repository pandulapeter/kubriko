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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectWatcherTest {
    private val root = createTempDirectory().toFile()
    private val generated = "src/commonMain/kotlin/game/generated"

    private fun write(path: String, text: String = "") = File(root, path).apply { parentFile.mkdirs(); writeText(text) }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun filesAreSortedIntoTheAreaTheyBelongTo() {
        assertEquals(ProjectChange.BLOCK_TYPE_DEFINITIONS, ProjectWatcher.areaOf(".kubriko/actors/Ball.json"))
        assertEquals(ProjectChange.SCENES, ProjectWatcher.areaOf("src/commonMain/composeResources/files/scenes/main.json"))
        assertEquals(ProjectChange.SOURCES, ProjectWatcher.areaOf("src/commonMain/kotlin/game/Game.kt"))
        assertEquals(ProjectChange.SOURCES, ProjectWatcher.areaOf("app/src/commonMain/composeResources/drawable/ball.png"))
        assertNull(ProjectWatcher.areaOf("build.gradle.kts"))
        assertNull(ProjectWatcher.areaOf(".kubriko/local/window.json"))
    }

    @Test
    fun onlyTheAreaThatChangedIsReported() {
        write(".kubriko/actors/Ball.json", "{}")
        write("src/commonMain/kotlin/game/Game.kt", "fun main() = Unit")
        val before = ProjectWatcher.snapshot(root, generated)
        write("src/commonMain/kotlin/game/Game.kt", "fun main() = println()")
        assertEquals(setOf(ProjectChange.SOURCES), ProjectWatcher.changedAreas(before, ProjectWatcher.snapshot(root, generated)))
        File(root, ".kubriko/actors/Ball.json").delete()
        assertEquals(setOf(ProjectChange.BLOCK_TYPE_DEFINITIONS), ProjectWatcher.changedAreas(before, ProjectWatcher.snapshot(root, generated)) - ProjectChange.SOURCES)
    }

    @Test
    fun filesAsCreatorLeftThemDoNotCountAsChanged() {
        val ownWrites = OwnWrites()
        val scene = write("src/commonMain/composeResources/files/scenes/main.json", "[]")
        val before = ProjectWatcher.snapshot(root, generated)
        scene.writeText("[ ]")
        ownWrites.record(scene)
        val isOwn = { path: String, stamp: FileStamp -> ownWrites.isOwn(File(root, path), stamp) }
        assertEquals(emptySet(), ProjectWatcher.changedAreas(before, ProjectWatcher.snapshot(root, generated), isOwn))
        scene.writeText("[  ]")
        scene.setLastModified(scene.lastModified() + 2_000)
        assertEquals(setOf(ProjectChange.SCENES), ProjectWatcher.changedAreas(before, ProjectWatcher.snapshot(root, generated), isOwn))
    }

    @Test
    fun creatorsOwnGeneratedCodeAndBuildOutputsAreIgnored() {
        write("src/commonMain/kotlin/game/Game.kt")
        val before = ProjectWatcher.snapshot(root, generated)
        write("$generated/Ball.kt", "class Ball")
        write("build/classes/Game.class", "binary")
        assertEquals(before, ProjectWatcher.snapshot(root, generated))
    }

    @Test
    fun aChangeIsReportedOnTheNextCheck() = runTest {
        write(".kubriko/actors/Ball.json", "{}")
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val changes = mutableListOf<Set<ProjectChange>>()
        val watching = ProjectWatcher(root, generated, CoroutineScope(backgroundScope.coroutineContext + dispatcher), dispatcher, intervalInMilliseconds = 1_000)
            .start { changes += it }
        advanceTimeBy(1_500)
        assertTrue(changes.isEmpty())
        write(".kubriko/actors/Wall.json", "{}")
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf(setOf(ProjectChange.BLOCK_TYPE_DEFINITIONS)), changes)
        watching.close()
        write(".kubriko/actors/Paddle.json", "{}")
        advanceTimeBy(5_000)
        assertEquals(1, changes.size)
    }
}

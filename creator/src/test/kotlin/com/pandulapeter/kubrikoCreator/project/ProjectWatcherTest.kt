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
import kotlin.test.assertNotEquals
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
    fun onlyTheAreaThatChangedGetsANewFingerprint() {
        write(".kubriko/actors/Ball.json", "{}")
        write("src/commonMain/kotlin/game/Game.kt", "fun main() = Unit")
        val before = ProjectWatcher.fingerprint(root, generated)
        write("src/commonMain/kotlin/game/Game.kt", "fun main() = println()")
        val after = ProjectWatcher.fingerprint(root, generated)
        assertNotEquals(before[ProjectChange.SOURCES], after[ProjectChange.SOURCES])
        assertEquals(before[ProjectChange.BLOCK_TYPE_DEFINITIONS], after[ProjectChange.BLOCK_TYPE_DEFINITIONS])
    }

    @Test
    fun creatorsOwnGeneratedCodeAndBuildOutputsAreIgnored() {
        write("src/commonMain/kotlin/game/Game.kt")
        val before = ProjectWatcher.fingerprint(root, generated)
        write("$generated/Ball.kt", "class Ball")
        write("build/classes/Game.class", "binary")
        assertEquals(before, ProjectWatcher.fingerprint(root, generated))
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

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.project.ProjectChange
import com.pandulapeter.kubrikoCreator.scenes.SceneChange
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectChangesTest {
    private val calls = mutableListOf<String>()
    private var scanCount = 0
    private val problems = mutableListOf<Throwable>()

    private fun TestScope.router(
        isUnconfined: Boolean = true,
        scan: suspend () -> Unit = { scanCount++ },
    ) = ProjectChanges(
        scope = CoroutineScope(
            backgroundScope.coroutineContext +
                (if (isUnconfined) UnconfinedTestDispatcher(testScheduler) else StandardTestDispatcher(testScheduler)) +
                CoroutineExceptionHandler { _, exception -> problems += exception },
        ),
        scanResources = scan,
        onScenesChanged = { calls += "scenes changed $it" },
        onSceneRenamed = { oldName, newName -> calls += "renamed $oldName to $newName" },
        onScenesChangedOnDisk = { calls += "scenes changed on disk" },
        markSceneOutdated = { calls += "scene outdated" },
        regenerate = { calls += "regenerate" },
        onCodeChanged = { calls += "code changed" },
        refreshGit = { calls += "git" },
        refreshFormatStatus = { calls += "format" },
        reloadTypes = { calls += "reload types" },
        refreshProjectUpgrade = { calls += "upgrade" },
    )

    private val afterSceneChange = listOf("regenerate", "code changed", "git")

    @Test
    fun aSceneEditRefreshesTheSceneViewButNotTheResources() = runTest {
        router().onSceneChanges(listOf(SceneChange.Written("main", isNew = false)))
        assertEquals(listOf("scenes changed [main]") + afterSceneChange, calls)
        assertEquals(0, scanCount)
    }

    @Test
    fun aNewSceneRefreshesTheResourcesToo() = runTest {
        router().onSceneChanges(listOf(SceneChange.Written("main", isNew = true)))
        assertEquals(listOf("scenes changed [main]") + afterSceneChange, calls)
        assertEquals(1, scanCount)
    }

    @Test
    fun aDeletedSceneRefreshesTheResourcesToo() = runTest {
        router().onSceneChanges(listOf(SceneChange.Deleted("main")))
        assertEquals(listOf("scenes changed [main]") + afterSceneChange, calls)
        assertEquals(1, scanCount)
    }

    @Test
    fun aMovedSceneIsFollowedByTheSceneView() = runTest {
        router().onSceneChanges(listOf(SceneChange.Moved("main", "levels/main")))
        assertEquals(listOf("renamed main to levels/main", "scenes changed [main, levels/main]") + afterSceneChange, calls)
        assertEquals(1, scanCount)
    }

    @Test
    fun aChangedFolderRefreshesTheResources() = runTest {
        router().onSceneChanges(listOf(SceneChange.FoldersChanged))
        assertEquals(listOf("scenes changed []") + afterSceneChange, calls)
        assertEquals(1, scanCount)
    }

    @Test
    fun savedTypesMarkTheSceneOutdatedOnlyWhenTheTypesChangedAndAlwaysRefreshGit() = runTest {
        val router = router()
        router.onTypesSaved(haveTypesChanged = true)
        assertEquals(listOf("scene outdated", "code changed", "git"), calls)
        calls.clear()
        router.onTypesSaved(haveTypesChanged = false)
        assertEquals(listOf("git"), calls)
        assertEquals(0, scanCount)
    }

    @Test
    fun theWatchersSettingsAndTypeChangesAreRouted() = runTest {
        val router = router()
        router.onProjectChanged(setOf(ProjectChange.SETTINGS))
        assertEquals(listOf("format", "git"), calls)
        calls.clear()
        router.onProjectChanged(setOf(ProjectChange.BLOCK_TYPE_DEFINITIONS))
        assertEquals(listOf("reload types", "git"), calls)
        assertEquals(0, scanCount)
    }

    @Test
    fun theWatchersSourceChangesMarkTheSceneOutdatedAndRefreshTheUpgradeCheck() = runTest {
        router().onProjectChanged(setOf(ProjectChange.SOURCES))
        assertEquals(listOf("scene outdated", "upgrade", "code changed", "git"), calls)
        assertEquals(1, scanCount)
    }

    @Test
    fun theWatchersSceneChangesRefreshTheOpenScene() = runTest {
        router().onProjectChanged(setOf(ProjectChange.SCENES))
        assertEquals(listOf("regenerate", "scenes changed on disk", "code changed", "git"), calls)
        assertEquals(1, scanCount)
    }

    @Test
    fun manyChangesInOneTurnCauseOneScan() = runTest {
        val router = router(isUnconfined = false)
        repeat(10) { router.onSceneChanges(listOf(SceneChange.Written("scene$it", isNew = true))) }
        runCurrent()
        assertEquals(1, scanCount)
    }

    @Test
    fun changesDuringAScanCauseOneMoreScan() = runTest {
        val router = router(isUnconfined = false) {
            scanCount++
            delay(100)
        }
        router.refreshResources()
        runCurrent()
        repeat(5) { router.refreshResources() }
        advanceTimeBy(1_000)
        assertEquals(2, scanCount)
    }

    @Test
    fun aFailingScanIsReportedAndTheNextOneStillRuns() = runTest {
        var isFailing = true
        val router = router {
            scanCount++
            if (isFailing) throw IllegalStateException("Broken.")
        }
        router.refreshResources()
        isFailing = false
        router.refreshResources()
        assertEquals(2, scanCount)
        assertEquals(listOf("Broken."), problems.map { it.message })
    }
}

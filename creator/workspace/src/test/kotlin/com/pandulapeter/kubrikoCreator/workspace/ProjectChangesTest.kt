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

import com.pandulapeter.kubrikoCreator.project.FileStamp
import com.pandulapeter.kubrikoCreator.project.ProjectChange
import com.pandulapeter.kubrikoCreator.project.ProjectSnapshot
import com.pandulapeter.kubrikoCreator.scenes.SceneChange
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectChangesTest {
    private val calls = mutableListOf<String>()
    private var scanCount = 0
    private val scannedSnapshots = mutableListOf<ProjectSnapshot?>()
    private val problems = mutableListOf<Throwable>()

    private fun TestScope.router(
        isUnconfined: Boolean = true,
        scope: CoroutineScope = CoroutineScope(
            backgroundScope.coroutineContext +
                (if (isUnconfined) UnconfinedTestDispatcher(testScheduler) else StandardTestDispatcher(testScheduler)) +
                CoroutineExceptionHandler { _, exception -> problems += exception },
        ),
        scan: suspend (ProjectSnapshot?) -> Unit = { scannedSnapshots += it; scanCount++ },
    ) = ProjectChanges(
        scope = scope,
        scanResources = scan,
        onScenesChanged = { calls += "scenes changed $it" },
        onSceneRenamed = { oldName, newName -> calls += "renamed $oldName to $newName" },
        onScenesChangedOnDisk = { calls += "scenes changed on disk" },
        markSceneOutdated = { calls += "scene outdated" },
        regenerate = { regenerate() },
        onCodeChanged = { calls += "code changed" },
        refreshGit = { calls += "git" },
        refreshFormatStatus = { calls += "format" },
        reloadTypes = { calls += "reload types" },
        refreshProjectUpgrade = { calls += "upgrade" },
    )

    private var regenerate: () -> Unit = { calls += "regenerate" }
    private val afterSceneChange = listOf("regenerate", "code changed", "git")

    @Test
    fun aPartFollowingSceneChangesThatThrowsLeavesTheOthersFollowingThem() = runTest {
        val problemsFound = listOf(IllegalStateException("Broken."), NotImplementedError("Missing."))
        (problemsFound + CancellationException("A read that timed out.")).forEach { failure ->
            calls.clear()
            regenerate = { throw failure }
            router().onSceneChanges(listOf(SceneChange.Moved("a", "b")))
            assertEquals(listOf("renamed a to b", "scenes changed [a, b]", "code changed", "git"), calls)
        }
        assertEquals<List<Throwable>>(problemsFound, problems)
    }

    @Test
    fun spriteSheetsAreFollowedFromWhatTheFirstScanReadOn() = runTest {
        val router = router()
        val bat = mapOf("bat" to SpriteSheet(frameWidth = 8, frameHeight = 8, frameCount = 2, framesPerRow = 2))
        router.onSpriteSheets(bat)
        router.onSpriteSheetsScanned(scanned = bat, now = bat)
        router.onSpriteSheets(bat)
        assertEquals(emptyList(), calls)
        router.onSpriteSheets(emptyMap())
        router.onSpriteSheets(emptyMap())
        assertEquals(listOf("regenerate", "scene outdated", "code changed"), calls)
    }

    @Test
    fun aSpriteSheetChangedWhileTheFirstScanEndedIsFollowed() = runTest {
        val router = router()
        val bat = mapOf("bat" to SpriteSheet(frameWidth = 8, frameHeight = 8, frameCount = 2, framesPerRow = 2))
        router.onSpriteSheetsScanned(scanned = emptyMap(), now = bat)
        router.onSpriteSheetsScanned(scanned = bat, now = bat)
        assertEquals(listOf("regenerate", "scene outdated", "code changed"), calls)
    }

    @Test
    fun aBurstOfSpriteImageWritesIsFollowedOnceItSettles() = runTest {
        val revision = MutableStateFlow(0)
        router(isUnconfined = false).followSpriteImageWrites(revision)
        runCurrent()
        repeat(3) {
            revision.value++
            advanceTimeBy(400)
        }
        assertEquals(emptyList(), calls)
        advanceTimeBy(1_100)
        assertEquals(listOf("scene outdated", "code changed"), calls)
    }

    @Test
    fun aHandlerThatThrowsLeavesTheOthersFollowingSceneChanges() = runTest {
        regenerate = { error("Broken.") }
        val scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler) + CoroutineExceptionHandler { _, _ -> error("Not reported.") })
        router(scope = scope).onSceneChanges(listOf(SceneChange.Written("main", isNew = false)))
        assertEquals(listOf("scenes changed [main]", "code changed", "git"), calls)
    }

    @Test
    fun nothingFollowsChangesOnceTheScopeIsCancelled() = runTest {
        val closing = CoroutineScope(Job() + CoroutineExceptionHandler { _, exception -> problems += exception })
        val router = router(scope = closing)
        regenerate = {
            closing.cancel()
            throw CancellationException("Closing.")
        }
        router.onSceneChanges(listOf(SceneChange.Written("main", isNew = false)))
        router.onProjectChanged(setOf(ProjectChange.SCENES))
        assertEquals(listOf("scenes changed [main]"), calls)
        assertEquals(emptyList(), problems)
    }

    @Test
    fun aScanStopsTheScansOnceTheScopeIsCancelled() = runTest {
        listOf(IllegalStateException("Broken while closing."), CancellationException("Closing.")).forEach { failure ->
            scanCount = 0
            val scope = CoroutineScope(backgroundScope.coroutineContext + Job(backgroundScope.coroutineContext[Job]) + UnconfinedTestDispatcher(testScheduler) + CoroutineExceptionHandler { _, exception -> problems += exception })
            lateinit var router: ProjectChanges
            router = router(scope = scope) {
                scanCount++
                router.refreshResources()
                scope.cancel()
                throw failure
            }
            router.refreshResources()
            assertEquals(1, scanCount)
        }
        assertEquals(emptyList(), problems)
    }

    @Test
    fun aScopeWithoutAHandlerIsRefused() = runTest {
        val scope = CoroutineScope(Job())
        assertFailsWith<IllegalStateException> { router(scope = scope) }
        scope.cancel()
    }

    @Test
    fun aPartFollowingAWatchedChangeOrSavedTypesThatThrowsLeavesTheOthersFollowingIt() = runTest {
        regenerate = { error("Broken.") }
        val router = router()
        router.onProjectChanged(setOf(ProjectChange.SCENES))
        assertEquals(listOf("scenes changed on disk", "code changed", "git"), calls)
        calls.clear()
        router.onTypesSaved(haveTypesChanged = true)
        assertEquals(listOf("scene outdated", "code changed", "git"), calls)
        assertEquals(listOf("Broken."), problems.map { it.message })
    }

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
    fun theWatchersBuildFileChangesOnlyRegroupTheResourcesByModule() = runTest {
        val snapshot = ProjectSnapshot(mapOf("shared/build.gradle.kts" to FileStamp(1, 1)))
        router().onProjectChanged(setOf(ProjectChange.MODULES), snapshot)
        assertEquals(listOf("git"), calls)
        assertEquals(listOf<ProjectSnapshot?>(snapshot), scannedSnapshots)
    }

    @Test
    fun aWatcherChangeIsScannedFromItsSnapshotWithoutAWalk() = runTest {
        val snapshot = ProjectSnapshot(mapOf("src/commonMain/kotlin/Game.kt" to FileStamp(1, 1)))
        router().onProjectChanged(setOf(ProjectChange.SOURCES), snapshot)
        assertEquals(listOf<ProjectSnapshot?>(snapshot), scannedSnapshots)
    }

    @Test
    fun aWalkWaitingForAScanWinsOverASnapshotTheWatcherMayHaveTakenBeforeIt() = runTest {
        val router = router(isUnconfined = false)
        router.refreshResources()
        router.onProjectChanged(setOf(ProjectChange.SCENES), ProjectSnapshot(emptyMap()))
        runCurrent()
        assertEquals(listOf<ProjectSnapshot?>(null), scannedSnapshots)
    }

    @Test
    fun theLatestOfSeveralWaitingSnapshotsIsScanned() = runTest {
        val router = router(isUnconfined = false)
        val latest = ProjectSnapshot(mapOf("src/commonMain/kotlin/Game.kt" to FileStamp(2, 2)))
        router.onProjectChanged(setOf(ProjectChange.SCENES), ProjectSnapshot(emptyMap()))
        router.onProjectChanged(setOf(ProjectChange.SOURCES), latest)
        runCurrent()
        assertEquals(listOf<ProjectSnapshot?>(latest), scannedSnapshots)
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

    @Test
    fun anErrorOrAStrayCancellationInAScanDoesntEndTheScans() = runTest {
        val failures = mutableListOf<Throwable>(NotImplementedError("Missing."), CancellationException("A read that timed out."))
        val router = router {
            scanCount++
            failures.removeFirstOrNull()?.let { throw it }
        }
        repeat(3) { router.refreshResources() }
        assertEquals(3, scanCount)
        assertEquals(listOf("Missing."), problems.map { it.message })
    }
}

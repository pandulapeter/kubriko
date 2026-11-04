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

import com.pandulapeter.kubrikoCreator.gradle.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameModuleChoiceTest {
    private val directory = testDirectory()
    private val localFolder = File(directory, LocalProjectStore.FOLDER)
    private val file = File(localFolder, GameModuleChoice.FILE_NAME)
    private var paths = listOf(":app:desktop", ":editor:model", ":editor:region")
    private var chosenCount = 0

    private fun TestScope.choice(findDesktopModulePaths: suspend () -> List<String> = { paths }): GameModuleChoice {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return GameModuleChoice(directory, CoroutineScope(backgroundScope.coroutineContext + Job() + dispatcher), dispatcher, findDesktopModulePaths) { chosenCount++ }
    }

    @Test
    fun theFirstModuleInGradlesOrderIsTheGameUntilAnotherIsChosen() = runTest {
        val choice = choice()
        assertEquals(":app:desktop", choice.await())
        assertEquals(GameModules(paths, ":app:desktop"), choice.modules.value)
        assertTrue(choice.modules.value?.isChoosable == true)
        choice.choose(":editor:model")
        assertEquals(":editor:model", choice.await())
        assertEquals(1, chosenCount)
        assertEquals("""{"module":":editor:model"}""", file.readText().filterNot { it.isWhitespace() })
    }

    @Test
    fun theChoiceIsKeptForTheNextTime() = runTest {
        choice().choose(":editor:region")
        assertEquals(":editor:region", choice().await())
    }

    @Test
    fun aSingleModuleIsNoChoice() = runTest {
        paths = listOf(":")
        val choice = choice()
        assertEquals(":", choice.await())
        assertEquals(false, choice.modules.value?.isChoosable)
    }

    @Test
    fun choosingTheSameModuleOrOneThatIsntThereChangesNothing() = runTest {
        val choice = choice()
        choice.choose(":app:desktop")
        choice.choose(":tools")
        assertEquals(":app:desktop", choice.await())
        assertEquals(0, chosenCount)
        assertFalse(file.exists())
    }

    @Test
    fun nothingCanBeChosenBeforeGradleNamesTheModules() = runTest {
        val lookup = CompletableDeferred<List<String>>()
        val choice = choice { lookup.await() }
        choice.choose(":editor:model")
        assertNull(choice.modules.value)
        lookup.complete(paths)
        assertEquals(":app:desktop", choice.await())
        assertEquals(0, chosenCount)
    }

    @Test
    fun aSavedModuleThatIsGoneMeansTheFirstUntilItIsBack() = runTest {
        choice().choose(":editor:model")
        paths = listOf(":app:desktop")
        assertEquals(":app:desktop", choice().await())
        paths = listOf(":app:desktop", ":editor:model")
        assertEquals(":editor:model", choice().await())
    }

    @Test
    fun anUnreadableSavedChoiceMeansTheFirstModule() = runTest {
        localFolder.mkdirs()
        file.writeText("{ not json")
        assertEquals(":app:desktop", choice().await())
    }

    @Test
    fun aProjectWithoutADesktopModuleIsRejectedWithAReason() = runTest {
        paths = emptyList()
        val exception = assertFailsWith<ProjectInspectionException> { choice().await() }
        assertEquals(ProjectProblem.NO_DESKTOP_MODULE, exception.reason)
    }

    @Test
    fun theModulesAreAskedForAgainAfterTheBuildFilesChangedOnlyOnceTheyAreNeeded() = runTest {
        var lookups = 0
        val choice = choice { lookups++; paths }
        choice.choose(":editor:region")
        choice.onBuildFilesChanged()
        assertEquals(1, lookups)
        paths = listOf(":app:desktop", ":editor:model")
        assertEquals(":app:desktop", choice.await(), "the chosen module is gone, and nothing else was saved")
        assertEquals(2, lookups)
        assertEquals(GameModules(paths, ":app:desktop"), choice.modules.value)
    }

    @Test
    fun theChosenModuleStaysChosenWhenTheModulesAreAskedForAgain() = runTest {
        val choice = choice()
        choice.choose(":editor:model")
        choice.onBuildFilesChanged()
        paths = paths + ":editor:sound"
        assertEquals(":editor:model", choice.await())
        assertEquals(GameModules(paths, ":editor:model"), choice.modules.value)
    }

    @Test
    fun aLookupReplacedWhileItRanIsNotShown() = runTest {
        val first = CompletableDeferred<List<String>>()
        var lookups = 0
        val choice = choice { if (++lookups == 1) first.await() else paths }
        choice.onBuildFilesChanged()
        assertEquals(":app:desktop", choice.await())
        first.complete(listOf(":old"))
        assertEquals(GameModules(paths, ":app:desktop"), choice.modules.value)
    }

    @Test
    fun lookupsReplacedBeforeTheyStartedDontStayInTheWorkspacesScope() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val job = Job()
        val choice = GameModuleChoice(directory, CoroutineScope(backgroundScope.coroutineContext + job + dispatcher), dispatcher, { paths }) {}
        choice.await()
        repeat(3) { choice.onBuildFilesChanged() }
        assertEquals(2, job.children.count(), "the loop saving the choice, and the one lookup that would run next")
        assertEquals(":app:desktop", choice.await())
    }

    @Test
    fun aLookupStillRunningIsCancelledWithTheWorkspace() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(backgroundScope.coroutineContext + Job() + dispatcher)
        var isCancelled = false
        GameModuleChoice(directory, scope, dispatcher, {
            try {
                awaitCancellation()
            } finally {
                isCancelled = true
            }
        }) {}
        scope.cancel()
        assertTrue(isCancelled)
    }

    @Test
    fun theLocalFolderIgnoresItselfInGit() = runTest {
        choice().choose(":editor:model")
        assertEquals("*\n", File(localFolder, ".gitignore").readText())
    }
}

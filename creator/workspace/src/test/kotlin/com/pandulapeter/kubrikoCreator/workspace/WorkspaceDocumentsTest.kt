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

import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.testing.FakeBlockTypeRepository
import com.pandulapeter.kubrikoCreator.testing.FakeProjectBuild
import com.pandulapeter.kubrikoCreator.testing.FakeSessionChannel
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceDocumentsTest {
    private val rootDirectory = testDirectory()
    private var scanCount = 0

    private fun TestScope.createController(): WorkspaceController {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return workspaceController(
            project = CreatorProject(rootDirectory = rootDirectory, name = "game"),
            build = FakeProjectBuild(),
            sessionChannel = FakeSessionChannel(),
            parentScope = CoroutineScope(backgroundScope.coroutineContext + dispatcher),
            ioDispatcher = dispatcher,
            scanResources = { _, _ ->
                scanCount++
                mapOf(ResourceGroup.DOCUMENTS to rootDirectory.walkTopDown().filter { it.isFile && it.extension == "md" }.map { file -> file.relativeTo(rootDirectory).invariantSeparatorsPath }.sorted().map { ProjectResource(ResourceGroup.DOCUMENTS, File(rootDirectory, it), it) }.toList())
            },
            blockTypeRepository = FakeBlockTypeRepository(),
            openGitRepository = { null },
        ).also { runCurrent() }
    }

    @Test
    fun theScannedDocumentsAreTheOnesCommandsKnow() = runTest {
        File(rootDirectory, "docs").mkdirs()
        File(rootDirectory, "docs/combat.md").writeText("# Combat\n")
        val controller = createController()
        assertEquals(listOf("docs/combat.md"), controller.commands.context.documents.paths())
    }

    @Test
    fun aCreatedDocumentOpensAndTheResourcesAreScannedAgain() = runTest {
        val controller = createController()
        val scansBefore = scanCount
        assertNull(controller.creation.createDocument("docs", "combat"))
        runCurrent()
        assertEquals("# combat\n", File(rootDirectory, "docs/combat.md").readText())
        assertEquals(WorkspaceSelection.Document("docs/combat.md"), controller.selection.current.value)
        assertEquals(scansBefore + 1, scanCount)
        assertEquals(listOf("docs/combat.md"), controller.commands.context.documents.paths())
    }

    @Test
    fun aDocumentNameThatCantBeUsedIsExplainedAndCreatesNothing() = runTest {
        val controller = createController()
        assertEquals(NameProblem.DOCUMENT_FIRST_CHARACTER, controller.creation.createDocument("", ".hidden")?.reason)
        assertNull(controller.commands.history.value.nextUndoCommandName)
    }

    @Test
    fun aShownDocumentStaysShownWhereverItIsMovedAndAnotherDoesntOpen() = runTest {
        File(rootDirectory, "README.md").writeText("# Game\n")
        File(rootDirectory, "notes.md").writeText("# Notes\n")
        val controller = createController()
        controller.selection.openDocument("README.md")
        assertNull(controller.creation.moveDocument("README.md", "docs/README.md"))
        runCurrent()
        assertEquals(WorkspaceSelection.Document("docs/README.md"), controller.selection.current.value)
        assertNull(controller.creation.moveDocument("notes.md", "docs/notes.md"))
        runCurrent()
        assertEquals(WorkspaceSelection.Document("docs/README.md"), controller.selection.current.value)
        assertEquals(CommandProblem.DOCUMENT_TAKEN, controller.creation.moveDocument("docs/notes.md", "docs/README.md")?.reason)
    }
}

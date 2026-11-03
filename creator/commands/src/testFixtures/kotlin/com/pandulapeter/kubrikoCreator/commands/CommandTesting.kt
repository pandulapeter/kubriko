/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.pandulapeter.kubrikoCreator.commands
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeRepository
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypes
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypesController
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypesListener
import com.pandulapeter.kubrikoCreator.actors.store.FailedWrite
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.git.GitController
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.template.GameTargets
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import com.pandulapeter.kubrikoCreator.testing.FakeBlockTypeRepository
import com.pandulapeter.kubrikoCreator.testing.FakeGitRepository
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

public fun TestScope.testScope() = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))

public fun TestScope.blockTypesController(vararg definitions: BlockTypeDefinition, repository: BlockTypeRepository = FakeBlockTypeRepository(definitions.toList())) = BlockTypesController(
    repository = repository,
    scope = testScope(),
    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    listener = object : BlockTypesListener {
        override fun onSaved(haveTypesChanged: Boolean) = Unit

        override fun onSaveFailed(failure: FailedWrite) = Unit
    },
)

public fun TestScope.gitController(repository: FakeGitRepository?) = GitController(
    openRepository = { repository },
    createRepository = { message -> FakeGitRepository().apply { commitAll(message) } },
    generatedPath = null,
    scope = testScope(),
    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    report = {},
)

public fun TestScope.commandContext(
    vararg definitions: BlockTypeDefinition,
    blockTypes: BlockTypes? = blockTypesController(*definitions),
    git: GitController = gitController(repository = null),
    rootDirectory: File = testDirectory(),
    console: List<ConsoleEntry> = emptyList(),
    runner: ProjectRunner = FakeProjectRunner(),
    audioFiles: AudioFiles? = null,
    spriteFiles: SpriteFiles? = null,
    report: (ConsoleEntry) -> Unit = {},
    onProjectUpgraded: () -> Unit = {},
    projectUpgrade: () -> TemplateUpgrade = { TemplateUpgrade.UpToDate },
    targets: GameTargets? = null,
    rejectChanges: () -> Rejection? = { null },
    ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
    sceneWriteText: (File, String) -> Unit = { file, text -> file.writeTextAtomically(text) },
    sceneReadText: (File) -> String = { file -> file.readText() },
) = CommandContext(
    project = CreatorProject(rootDirectory = rootDirectory, name = "game"),
    console = MutableStateFlow(console),
    blockTypes = blockTypes,
    git = git,
    scenes = SceneStore(rootDirectory, writeText = sceneWriteText, readText = sceneReadText) { emptyList() },
    runner = runner,
    ioDispatcher = ioDispatcher,
    audioFiles = { audioFiles },
    spriteFiles = { spriteFiles },
    report = report,
    buildFiles = object : ProjectBuildFiles {
        override fun current() = projectUpgrade()

        override fun onUpgraded() = onProjectUpgraded()

        override val targets = targets
    },
    rejectChanges = rejectChanges,
)

/**
 * Applies [invocation], checks that its inverse reverts it and that the inverse of the inverse makes it again, and
 * returns what [snapshot] says after the change.
 */
public suspend fun <T> CommandContext.assertRoundTrip(invocation: Invocation<*>, snapshot: () -> T): T {
    val before = snapshot()
    assertNull(invocation.validate(this), "${invocation.command.name} was rejected.")
    val inverse = assertNotNull(invocation.apply(this).inverse).copy(isRestoring = true)
    val after = snapshot()
    assertNotEquals(before, after, "${invocation.command.name} changed nothing.")
    assertNull(inverse.validate(this), "The inverse of ${invocation.command.name} was rejected.")
    val reapply = assertNotNull(inverse.apply(this).inverse).copy(isRestoring = true)
    assertEquals(before, snapshot(), "The inverse of ${invocation.command.name} didn't revert it.")
    assertNull(reapply.validate(this))
    reapply.apply(this)
    assertEquals(after, snapshot(), "The inverse of the inverse of ${invocation.command.name} didn't make the change again.")
    return after
}

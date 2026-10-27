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

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.git.FakeGitRepository
import com.pandulapeter.kubrikoCreator.git.GitController
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.workspace.BlockTypesController
import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import com.pandulapeter.kubrikoCreator.workspace.FakeBlockTypeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal fun TestScope.testScope() = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))

internal fun TestScope.blockTypesController(vararg definitions: BlockTypeDefinition, repository: FakeBlockTypeRepository = FakeBlockTypeRepository(definitions.toList())) = BlockTypesController(
    repository = repository,
    scope = testScope(),
    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    onSaved = {},
    onSaveFailed = {},
)

internal fun TestScope.gitController(repository: FakeGitRepository?) = GitController(
    openRepository = { repository },
    createRepository = { message -> FakeGitRepository().apply { commitAll(message) } },
    generatedPath = null,
    scope = testScope(),
    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    report = {},
)

internal fun TestScope.commandContext(
    vararg definitions: BlockTypeDefinition,
    blockTypes: BlockTypesController? = blockTypesController(*definitions),
    git: GitController = gitController(repository = null),
    rootDirectory: File = createTempDirectory().toFile(),
    console: List<ConsoleEntry> = emptyList(),
    runner: ProjectRunner = FakeProjectRunner(),
    onScenesChanged: (Collection<String>) -> Unit = {},
    onProjectUpgraded: () -> Unit = {},
) = CommandContext(
    project = CreatorProject(rootDirectory = rootDirectory, name = "game", desktopModulePath = ":"),
    console = MutableStateFlow(console),
    blockTypes = blockTypes,
    git = git,
    scenes = SceneStore(rootDirectory) { emptyList() },
    runner = runner,
    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    onScenesChanged = onScenesChanged,
    onProjectUpgraded = onProjectUpgraded,
)

/**
 * Applies [invocation], checks that its inverse reverts it and that the inverse of the inverse makes it again, and
 * returns what [snapshot] says after the change.
 */
internal suspend fun <T> CommandContext.assertRoundTrip(invocation: Invocation<*>, snapshot: () -> T): T {
    val before = snapshot()
    assertNull(invocation.validate(this), "${invocation.command.name} was rejected.")
    val inverse = assertNotNull(invocation.apply(this).inverse)
    val after = snapshot()
    assertNotEquals(before, after, "${invocation.command.name} changed nothing.")
    assertNull(inverse.validate(this), "The inverse of ${invocation.command.name} was rejected.")
    val reapply = assertNotNull(inverse.apply(this).inverse)
    assertEquals(before, snapshot(), "The inverse of ${invocation.command.name} didn't revert it.")
    assertNull(reapply.validate(this))
    reapply.apply(this)
    assertEquals(after, snapshot(), "The inverse of the inverse of ${invocation.command.name} didn't make the change again.")
    return after
}

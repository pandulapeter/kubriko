/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.git.GitController
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.workspace.ActorTypesController
import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow

/**
 * What commands read and change: the parts of the open project.
 *
 * @param actorTypes The project's block-built Actor types, or null if the project hasn't opted in to them.
 * @param onScenesChanged Tells the rest of the editor which Scene files a command changed.
 */
internal class CommandContext(
    val project: CreatorProject,
    val console: StateFlow<List<ConsoleEntry>>,
    val actorTypes: ActorTypesController?,
    val git: GitController,
    val scenes: SceneStore,
    val runner: ProjectRunner,
    val ioDispatcher: CoroutineDispatcher,
    val onScenesChanged: (names: Collection<String>) -> Unit,
)

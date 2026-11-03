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
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.project.ProjectChange
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ProjectSnapshot
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.template.GameTargets
import java.io.File

/**
 * How the workspace notices what changed in the project's files, and changes its build files.
 *
 * @param ownWrites The files as Creator itself last wrote them, which don't count as changed.
 * @param scanResources Builds the resource tree, from the watcher's snapshot when one is given.
 * @param startWatching Starts watching the project for outside changes, until the returned handle is closed.
 * @param targets Turns the game's targets on and off in its build files, or null if they can't be changed here.
 */
public class WorkspaceFiles(
    internal val ownWrites: OwnWrites = OwnWrites(),
    internal val scanResources: (File, ProjectSnapshot?) -> Map<ResourceGroup, List<ProjectResource>> = { root, snapshot -> ResourceScanner.scan(root, snapshot = snapshot) },
    internal val startWatching: (onChange: (Set<ProjectChange>, ProjectSnapshot) -> Unit) -> AutoCloseable = { AutoCloseable {} },
    internal val targets: GameTargets? = null,
)

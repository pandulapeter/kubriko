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
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeRepository
import com.pandulapeter.kubrikoCreator.git.GitRepository
import com.pandulapeter.kubrikoCreator.git.JGitRepository
import com.pandulapeter.kubrikoCreator.gradle.ProjectBuild
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFileChanges
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.project.ProjectChange
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ProjectSnapshot
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.sceneRuntime.ClassLoaderEntryPointLoader
import com.pandulapeter.kubrikoCreator.sceneRuntime.EntryPointLoader
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneRuntimeSetup
import com.pandulapeter.kubrikoCreator.session.GameLaunch
import com.pandulapeter.kubrikoCreator.session.GameProcess
import com.pandulapeter.kubrikoCreator.session.SessionChannel
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import com.pandulapeter.kubrikoCreator.testing.FakeProjectBuild
import com.pandulapeter.kubrikoCreator.testing.FakeSessionChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import java.io.File

/**
 * Builds a workspace on a fake Gradle connection and session channel, with the controller's production defaults for
 * everything else unless told otherwise, taking each part's parameters flat. Block types are set up only when
 * [blockTypeRepository] is given.
 */
public fun workspaceController(
    project: CreatorProject,
    parentScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    build: ProjectBuild = FakeProjectBuild(),
    sessionChannel: SessionChannel = FakeSessionChannel(),
    findDesktopModulePath: suspend () -> String = { ":" },
    runGame: suspend (GameLaunch, Map<String, String>) -> GameProcess.Exit = GameProcess(project.rootDirectory, SessionInitScript.playDirectoryOf(project.rootDirectory))::run,
    ownWrites: OwnWrites = OwnWrites(),
    scanResources: (File, ProjectSnapshot?) -> Map<ResourceGroup, List<ProjectResource>> = { root, snapshot -> ResourceScanner.scan(root, snapshot = snapshot) },
    startWatching: (onChange: (Set<ProjectChange>, ProjectSnapshot) -> Unit) -> AutoCloseable = { AutoCloseable {} },
    blockTypeRepository: BlockTypeRepository? = null,
    generatedPath: String? = null,
    syncBuildFiles: () -> KubrikoBuildFileChanges? = { null },
    writeDelayInMilliseconds: Long = 0,
    creatorKubrikoVersion: String = "",
    creatorLibraries: Map<String, String> = emptyMap(),
    entryPointLoader: EntryPointLoader = ClassLoaderEntryPointLoader(project.rootDirectory),
    initialFormatStatus: ProjectFormatStatus = ProjectFormatStatus.Writable,
    openGitRepository: () -> GitRepository? = { JGitRepository.open(project.rootDirectory) },
    createGitRepository: (message: String) -> GitRepository = { JGitRepository.create(project.rootDirectory, it) },
): WorkspaceController = WorkspaceController(
    project = project,
    processes = ProjectProcesses(build, sessionChannel, runGame, findDesktopModulePath),
    parentScope = parentScope,
    ioDispatcher = ioDispatcher,
    files = WorkspaceFiles(ownWrites, scanResources, startWatching),
    blockTypeSetup = blockTypeRepository?.let { BlockTypesSetup(it, generatedPath, syncBuildFiles, writeDelayInMilliseconds) },
    sceneRuntime = SceneRuntimeSetup(creatorKubrikoVersion, creatorLibraries, entryPointLoader),
    initialFormatStatus = initialFormatStatus,
    gitRepositories = GitRepositories(openGitRepository, createGitRepository),
)

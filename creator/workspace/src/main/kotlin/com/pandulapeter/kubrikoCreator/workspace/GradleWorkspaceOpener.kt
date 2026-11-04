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
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeStore
import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.foundation.CreatorLibraries
import com.pandulapeter.kubrikoCreator.gradle.DesktopModules
import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
import com.pandulapeter.kubrikoCreator.gradle.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFiles
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.project.ProjectFormat
import com.pandulapeter.kubrikoCreator.project.ProjectInspector
import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import com.pandulapeter.kubrikoCreator.project.ProjectWatcher
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.sceneRuntime.ClassLoaderEntryPointLoader
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneRuntimeSetup
import com.pandulapeter.kubrikoCreator.session.GameProcess
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import com.pandulapeter.kubrikoCreator.session.SessionServer
import com.pandulapeter.kubrikoCreator.template.GameTargets
import com.pandulapeter.kubrikoCreator.template.KubrikoAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Opens a workspace through the project's own Gradle build, without waiting for Gradle: only the project's files are
 * read, and the desktop module is asked from Gradle in the background, for the first build to wait for if it must.
 * That first build starts right away, in the background too, so that the Gradle daemon is warm and the project's
 * classes are loaded by the time a Scene is opened. The session server and the watcher run in the workspace's own scope,
 * and an opening that fails closes everything it started.
 *
 * @param configuration The Kubriko version and repositories Creator bundles.
 * @param loadCreatorLibraries The libraries Creator runs with, which the app bundles into its resources.
 */
public class GradleWorkspaceOpener(
    private val configuration: CreatorConfiguration,
    private val loadCreatorLibraries: () -> Map<String, String> = CreatorLibraries::load,
    private val isKubrikoAvailable: (String) -> Boolean = KubrikoAvailability.of(configuration),
) : WorkspaceOpener {

    override suspend fun open(directory: File, scope: CoroutineScope): WorkspaceController {
        if (!withContext(Dispatchers.IO) { ProjectInspector.isGradleProject(directory) }) throw ProjectInspectionException(ProjectProblem.NOT_A_GRADLE_PROJECT)
        val project = withContext(Dispatchers.IO) { CreatorProject(rootDirectory = directory, name = ProjectInspector.nameOf(directory)) }
        val ownWrites = OwnWrites()
        val creatorLibraries = withContext(Dispatchers.IO) { loadCreatorLibraries() }
        val projectFormat = ProjectFormat(directory, configuration.kubrikoVersion)
        val formatStatus = try {
            withContext(Dispatchers.IO) { projectFormat.prepare() }
        } catch (_: IOException) {
            throw ProjectInspectionException(ProjectProblem.FORMAT_NOT_UPGRADED_ON_OPEN)
        }
        val blockTypeStore = withContext(Dispatchers.IO) { BlockTypeStore.open(directory, ownWrites) }
        return openWorkspace(scope) { opening ->
            val connection = opening.track(GradleProjectConnection(directory))
            WorkspaceController(
                project = project,
                processes = ProjectProcesses(
                    build = connection,
                    sessionChannel = opening.track(SessionServer(opening.scope)),
                    runGame = GameProcess(directory, SessionInitScript.playDirectoryOf(directory))::run,
                    findDesktopModulePaths = { DesktopModules.pathsOf(connection) },
                ),
                parentScope = scope,
                workspaceScope = opening.scope,
                files = WorkspaceFiles(
                    ownWrites = ownWrites,
                    scanResources = { root, snapshot -> ResourceScanner.scan(root, hiddenDirectory = blockTypeStore?.generatedPath, snapshot = snapshot) },
                    startWatching = { onChange ->
                        opening.track(ProjectWatcher(directory, blockTypeStore?.generatedPath, opening.scope, Dispatchers.IO, ownWrites = ownWrites).start(onChange))
                    },
                    targets = GameTargets(directory, configuration.android),
                ),
                blockTypeSetup = blockTypeStore?.let { store ->
                    BlockTypesSetup(
                        repository = store,
                        generatedPath = store.generatedPath,
                        syncBuildFiles = KubrikoBuildFiles(directory, configuration.kubrikoVersion, configuration.useMavenLocal, isKubrikoAvailable)::sync,
                        writeDelayInMilliseconds = WRITE_DELAY_IN_MILLISECONDS,
                    )
                },
                sceneRuntime = SceneRuntimeSetup(
                    creatorKubrikoVersion = configuration.kubrikoVersion,
                    creatorLibraries = creatorLibraries,
                    entryPointLoader = ClassLoaderEntryPointLoader(directory),
                ),
                initialFormatStatus = formatStatus,
                projectFormat = projectFormat,
            ).also { it.sceneEditing.prepare() }
        }
    }

    private companion object {
        /**
         * How long changes wait to be written together with the next ones, which typing into a field makes many of.
         */
        const val WRITE_DELAY_IN_MILLISECONDS = 100L
    }
}

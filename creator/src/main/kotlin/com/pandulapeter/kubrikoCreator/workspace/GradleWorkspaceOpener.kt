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

import com.pandulapeter.kubrikoCreator.actors.BlockTypeStore
import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.foundation.CreatorLibraries
import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFiles
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.project.ProjectFormat
import com.pandulapeter.kubrikoCreator.project.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.project.ProjectInspector
import com.pandulapeter.kubrikoCreator.project.ProjectWatcher
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.session.SessionServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_not_a_gradle_project
import kubriko.creator.generated.resources.error_project_format_not_upgraded_on_open
import java.io.File
import java.io.IOException

/**
 * Opens a workspace through the project's own Gradle build, without waiting for Gradle: only the project's files are
 * read, and the desktop module is asked from Gradle in the background, for the first build to wait for if it must.
 * That first build starts right away, in the background too, so that the Gradle daemon is warm and the project's
 * classes are loaded by the time a Scene is opened.
 */
internal object GradleWorkspaceOpener : WorkspaceOpener {

    override suspend fun open(directory: File, scope: CoroutineScope): WorkspaceController {
        if (!withContext(Dispatchers.IO) { ProjectInspector.isGradleProject(directory) }) throw ProjectInspectionException(Res.string.error_not_a_gradle_project)
        val project = withContext(Dispatchers.IO) { CreatorProject(rootDirectory = directory, name = ProjectInspector.nameOf(directory)) }
        val ownWrites = OwnWrites()
        val configuration = CreatorConfiguration.load()
        val creatorLibraries = withContext(Dispatchers.IO) { CreatorLibraries.load() }
        val projectFormat = ProjectFormat(directory, configuration.kubrikoVersion)
        val formatStatus = try {
            withContext(Dispatchers.IO) { projectFormat.prepare() }
        } catch (_: IOException) {
            throw ProjectInspectionException(Res.string.error_project_format_not_upgraded_on_open)
        }
        val blockTypeStore = withContext(Dispatchers.IO) { BlockTypeStore.open(directory, ownWrites) }
        val connection = GradleProjectConnection(directory)
        return WorkspaceController(
            project = project,
            build = connection,
            sessionChannel = SessionServer(scope),
            parentScope = scope,
            findDesktopModulePath = { ProjectInspector.desktopModulePathOf(connection) },
            scanResources = { ResourceScanner.scan(it, hiddenDirectory = blockTypeStore?.generatedPath) },
            blockTypeRepository = blockTypeStore,
            syncBuildFiles = KubrikoBuildFiles(directory, configuration.kubrikoVersion, configuration.useMavenLocal)::sync,
            creatorKubrikoVersion = configuration.kubrikoVersion,
            creatorLibraries = creatorLibraries,
            initialFormatStatus = formatStatus,
            projectFormat = projectFormat,
            generatedPath = blockTypeStore?.generatedPath,
            writeDelayInMilliseconds = WRITE_DELAY_IN_MILLISECONDS,
            ownWrites = ownWrites,
            startWatching = { onChange ->
                ProjectWatcher(directory, blockTypeStore?.generatedPath, scope, Dispatchers.IO, ownWrites = ownWrites).start(onChange)
            },
        ).also { it.sceneEditing.prepare() }
    }

    /**
     * How long changes wait to be written together with the next ones, which typing into a field makes many of.
     */
    private const val WRITE_DELAY_IN_MILLISECONDS = 100L
}

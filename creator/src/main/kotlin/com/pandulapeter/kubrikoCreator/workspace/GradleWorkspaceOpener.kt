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

import com.pandulapeter.kubrikoCreator.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.actors.BlockTypeStore
import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFiles
import com.pandulapeter.kubrikoCreator.project.ProjectInspector
import com.pandulapeter.kubrikoCreator.project.ProjectWatcher
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.session.SessionServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Opens a workspace through the project's own Gradle build.
 */
internal object GradleWorkspaceOpener : WorkspaceOpener {

    override suspend fun open(directory: File, scope: CoroutineScope) = withContext(Dispatchers.IO) {
        val connection = GradleProjectConnection(directory)
        try {
            val blockTypeStore = BlockTypeStore.open(directory)
            val configuration = CreatorConfiguration.load()
            val buildFiles = KubrikoBuildFiles(directory, configuration.kubrikoVersion, configuration.useMavenLocal)
            WorkspaceController(
                project = ProjectInspector.inspect(connection),
                build = connection,
                sessionChannel = SessionServer(scope),
                scope = scope,
                scanResources = { ResourceScanner.scan(it, hiddenDirectory = blockTypeStore?.generatedPath) },
                blockTypeRepository = blockTypeStore,
                syncBuildFiles = buildFiles::sync,
                creatorKubrikoVersion = configuration.kubrikoVersion,
                generatedPath = blockTypeStore?.generatedPath,
                startWatching = { onChange ->
                    ProjectWatcher(directory, blockTypeStore?.generatedPath, scope, Dispatchers.IO).start(onChange)
                },
            )
        } catch (exception: Exception) {
            connection.close()
            throw exception
        }
    }
}

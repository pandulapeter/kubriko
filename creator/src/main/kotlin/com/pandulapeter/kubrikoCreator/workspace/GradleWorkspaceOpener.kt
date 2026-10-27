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
import com.pandulapeter.kubrikoCreator.actors.ActorTypeStore
import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
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
            val actorTypeStore = ActorTypeStore.open(directory)
            WorkspaceController(
                project = ProjectInspector.inspect(connection),
                build = connection,
                sessionChannel = SessionServer(scope),
                scope = scope,
                scanResources = { ResourceScanner.scan(it, hiddenDirectory = actorTypeStore?.generatedPath) },
                actorTypeRepository = actorTypeStore,
                creatorKubrikoVersion = CreatorConfiguration.load().kubrikoVersion,
                startWatching = { onChange ->
                    ProjectWatcher(directory, actorTypeStore?.generatedPath, scope, Dispatchers.IO).start(onChange)
                },
            )
        } catch (exception: Exception) {
            connection.close()
            throw exception
        }
    }
}

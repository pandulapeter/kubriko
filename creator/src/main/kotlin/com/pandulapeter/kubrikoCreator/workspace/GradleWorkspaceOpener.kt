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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeStore
import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
import com.pandulapeter.kubrikoCreator.project.ProjectInspector
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
            WorkspaceController(
                project = ProjectInspector.inspect(connection),
                build = connection,
                sessionChannel = SessionServer(scope),
                scope = scope,
                actorTypeRepository = ActorTypeStore.open(directory),
            )
        } catch (exception: Exception) {
            connection.close()
            throw exception
        }
    }
}

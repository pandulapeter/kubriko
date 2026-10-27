/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator

import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import org.jetbrains.compose.resources.StringResource
import java.io.File

/**
 * What the Kubriko Creator window shows.
 */
internal sealed interface AppScreen {

    data class Welcome(
        val error: StringResource? = null,
    ) : AppScreen

    data class NewProject(
        val error: StringResource? = null,
    ) : AppScreen

    data class CreatingProject(
        val directory: File,
    ) : AppScreen

    data class Opening(
        val directory: File,
    ) : AppScreen

    data class Workspace(
        val controller: WorkspaceController,
    ) : AppScreen
}

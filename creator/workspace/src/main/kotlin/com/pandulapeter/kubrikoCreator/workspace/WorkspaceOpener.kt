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

import kotlinx.coroutines.CoroutineScope
import java.io.File

/**
 * Turns a project folder into a [WorkspaceController], or throws.
 */
public fun interface WorkspaceOpener {

    /**
     * Opens the project in [directory], running everything it starts in a child of [scope].
     */
    public suspend fun open(directory: File, scope: CoroutineScope): WorkspaceController
}

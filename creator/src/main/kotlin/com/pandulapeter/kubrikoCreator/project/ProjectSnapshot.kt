/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

/**
 * The files of a project as [ProjectWatcher] last found them: the stamp of every file the watcher or [ResourceScanner]
 * cares about, by its path relative to the project's root.
 */
internal data class ProjectSnapshot(
    val stamps: Map<String, FileStamp>,
)

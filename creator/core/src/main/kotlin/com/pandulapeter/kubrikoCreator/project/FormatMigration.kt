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

import java.io.File

/**
 * Brings a project's Kubriko Creator files (`.kubriko` and the Scenes) from one format version to the next.
 * A raise that only adds to the format is an empty migration.
 */
public fun interface FormatMigration {

    /**
     * Rewrites the files of the project in [rootDirectory], which have the format version before this migration's.
     */
    public fun migrate(rootDirectory: File)
}

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.store

import com.pandulapeter.kubrikoCreator.project.FormatMigration
import java.io.File

/**
 * Format version 21 added sound and music Properties, which the play sound and play music Blocks can play, written as an
 * object where a fixed file stays its bare path. It only adds to the format, so nothing is rewritten.
 */
internal object AudioPropertiesMigration : FormatMigration {

    override fun migrate(rootDirectory: File) = Unit
}

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
 * Format version 22 added the camera Blocks: follow with the camera, move, shake, and zoom the camera. It only adds to the
 * format, so nothing is rewritten.
 */
internal object CameraMigration : FormatMigration {

    override fun migrate(rootDirectory: File) = Unit
}

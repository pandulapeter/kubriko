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
 * Format version 5: every number a Block takes can be a random number or a Property's value
 * ([com.pandulapeter.kubrikoCreator.actors.NumberValue]), written as an object where a bare number was, which an older
 * Kubriko Creator can't read. A fixed number is still a bare number, so nothing on disk changes; an older Kubriko Creator
 * opens the project read-only.
 */
internal object NumberValuesMigration : FormatMigration {

    override fun migrate(rootDirectory: File) = Unit
}

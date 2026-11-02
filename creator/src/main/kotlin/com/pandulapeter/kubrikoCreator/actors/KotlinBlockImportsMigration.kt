/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.project.FormatMigration
import java.io.File

/**
 * Format version 2: the import lines a Kotlin Block starts with become imports of the generated file, which an older
 * Kubriko Creator would put inside a function, generating code that doesn't compile. Nothing on disk changes; an older
 * Kubriko Creator opens the project read-only.
 */
internal object KotlinBlockImportsMigration : FormatMigration {

    override fun migrate(rootDirectory: File) = Unit
}

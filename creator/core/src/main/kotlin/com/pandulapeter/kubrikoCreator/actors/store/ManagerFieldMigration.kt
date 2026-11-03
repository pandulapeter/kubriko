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

import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.withManagerFieldsSuffixed
import com.pandulapeter.kubrikoCreator.project.FormatMigration
import java.io.File

/**
 * Format version 1: the generated code looks every other Manager type up in a field named `<name>Manager`
 * ([BlockScope.managerField]), so the Kotlin Blocks and Kotlin UI that named the old fields are renamed to it
 * ([withManagerFieldsSuffixed]). Type files that can't be read are left as they are.
 */
internal object ManagerFieldMigration : FormatMigration {

    override fun migrate(rootDirectory: File) {
        val store = BlockTypeStore.open(rootDirectory) ?: return
        val types = store.load().types
        types.forEach { definition ->
            val migrated = definition.withManagerFieldsSuffixed(types)
            if (migrated != definition) store.save(migrated)
        }
    }
}

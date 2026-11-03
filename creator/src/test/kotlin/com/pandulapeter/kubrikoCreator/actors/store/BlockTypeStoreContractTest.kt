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

import com.pandulapeter.kubrikoCreator.testing.BlockTypeRepositoryContract
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File

internal class BlockTypeStoreContractTest : BlockTypeRepositoryContract() {
    override fun repository(): BlockTypeRepository {
        val root = testDirectory()
        File(root, ".kubriko/project.json").apply { parentFile.mkdirs() }.writeText("""{ "packageName": "com.example.game" }""")
        return BlockTypeStore.open(root)!!
    }
}

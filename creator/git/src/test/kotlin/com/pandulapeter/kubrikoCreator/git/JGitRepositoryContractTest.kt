/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.git

import com.pandulapeter.kubrikoCreator.testing.GitRepositoryContract
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File

internal class JGitRepositoryContractTest : GitRepositoryContract() {
    private val root = testDirectory()

    override fun repository(): GitRepository {
        listOf("kept.txt", "changed.txt", "deleted.txt").forEach { File(root, it).writeText("a") }
        return JGitRepository.create(root, "Created.")
    }

    override fun change(repository: GitRepository, path: String, kind: ChangeKind) {
        val file = File(root, path)
        when (kind) {
            ChangeKind.ADDED -> file.writeText("new")
            ChangeKind.MODIFIED -> file.writeText("changed")
            ChangeKind.DELETED -> file.delete()
            ChangeKind.CONFLICTING -> error("Conflicts aren't part of the contract.")
        }
    }
}

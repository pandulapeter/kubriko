/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import com.pandulapeter.kubrikoCreator.git.ChangeKind
import com.pandulapeter.kubrikoCreator.git.FileChange
import com.pandulapeter.kubrikoCreator.git.GitRepository

internal class FakeGitRepositoryContractTest : GitRepositoryContract() {
    override fun repository() = FakeGitRepository().apply { commitAll("Created.") }

    override fun change(repository: GitRepository, path: String, kind: ChangeKind) {
        val fake = repository as FakeGitRepository
        fake.status = fake.status.copy(changes = (fake.status.changes + FileChange(path, kind)).sortedBy { it.path })
    }
}

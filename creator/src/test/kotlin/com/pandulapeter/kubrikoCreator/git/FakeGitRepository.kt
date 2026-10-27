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

import org.eclipse.jgit.api.errors.NoFilepatternException

internal class FakeGitRepository(
    var status: RepositoryStatus = RepositoryStatus(branch = "main", changes = emptyList()),
) : GitRepository {
    val commits = mutableListOf<String>()
    val discarded = mutableListOf<Collection<String>>()
    var isFailing = false

    override fun status() = if (isFailing) throw NoFilepatternException("Broken.") else status

    override fun commitAll(message: String) {
        if (isFailing) throw NoFilepatternException("Broken.")
        commits += message
        status = status.copy(changes = emptyList())
    }

    override fun history(limit: Int) = commits.reversed().take(limit).mapIndexed { index, message -> CommitSummary("$index", message, "Me", 0L) }

    override fun discard(paths: Collection<String>) {
        discarded += paths
        status = status.copy(changes = status.changes.filter { it.path !in paths })
    }
}

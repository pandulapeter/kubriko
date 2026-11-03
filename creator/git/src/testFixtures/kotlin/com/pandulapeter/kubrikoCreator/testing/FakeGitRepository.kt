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

import com.pandulapeter.kubrikoCreator.git.CommitSummary
import com.pandulapeter.kubrikoCreator.git.GitRepository
import com.pandulapeter.kubrikoCreator.git.RepositoryStatus
import org.eclipse.jgit.api.errors.NoFilepatternException

/**
 * A [GitRepository] in memory, which records commits and discards and fails every call while [isFailing].
 *
 * @param status What [status] returns until a commit or a discard changes it.
 */
public class FakeGitRepository(
    public var status: RepositoryStatus = RepositoryStatus(branch = "main", changes = emptyList()),
) : GitRepository {
    /**
     * The messages of every commit, oldest first.
     */
    public val commits: MutableList<String> = mutableListOf()

    /**
     * The paths of every discard, in order.
     */
    public val discarded: MutableList<Collection<String>> = mutableListOf()

    /**
     * Whether every call but [discard] throws [failure].
     */
    public var isFailing: Boolean = false

    /**
     * What a failing call throws: JGit's exception by default, as the real repository would.
     */
    public var failure: () -> Exception = { NoFilepatternException("Broken.") }

    /**
     * How many times [status] was called.
     */
    public var statusCalls: Int = 0

    override fun status(): RepositoryStatus {
        statusCalls++
        return if (isFailing) throw failure() else status
    }

    override fun commitAll(message: String) {
        if (isFailing) throw failure()
        commits += message
        status = status.copy(changes = emptyList())
    }

    override fun history(limit: Int): List<CommitSummary> = commits.reversed().take(limit).mapIndexed { index, message -> CommitSummary("$index", message, "Me", 0L) }

    override fun discard(paths: Collection<String>) {
        discarded += paths
        status = status.copy(changes = status.changes.filter { it.path !in paths })
    }
}

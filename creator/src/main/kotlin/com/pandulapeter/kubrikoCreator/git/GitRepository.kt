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

/**
 * The git operations Kubriko Creator offers. Every call blocks, so callers run them off the main thread.
 */
internal interface GitRepository {

    fun status(): RepositoryStatus

    /**
     * Records every change (new, changed, and deleted files alike) in a new commit.
     */
    fun commitAll(message: String)

    /**
     * The most recent commits, newest first; nothing for a repository without commits.
     */
    fun history(limit: Int): List<CommitSummary>

    /**
     * Restores [paths] to how the last commit has them, deleting the ones it doesn't have.
     */
    fun discard(paths: Collection<String>)
}

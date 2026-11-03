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
 * What the version control panel shows.
 */
public sealed interface GitState {

    /**
     * The repository is still being read.
     */
    public data object Loading : GitState

    /**
     * The project isn't the root of a git repository.
     */
    public data object NoRepository : GitState

    /**
     * The repository as it is now.
     *
     * @param branch The checked-out branch, or null when no branch is checked out.
     * @param history The most recent commits, newest first.
     */
    public data class Ready(
        public val branch: String?,
        public val changes: List<ChangeEntry>,
        public val history: List<CommitSummary>,
    ) : GitState
}

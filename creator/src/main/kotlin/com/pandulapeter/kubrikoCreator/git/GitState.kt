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
internal sealed interface GitState {

    data object Loading : GitState

    /**
     * The project isn't the root of a git repository.
     */
    data object NoRepository : GitState

    data class Ready(
        val branch: String?,
        val changes: List<ChangeEntry>,
        val history: List<CommitSummary>,
    ) : GitState
}

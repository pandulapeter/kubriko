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
 * What asking for a commit did, decided from the repository's status at that moment rather than from the last [GitState].
 */
public sealed interface CommitResult {

    /**
     * Every change is in a new commit.
     */
    public data object Committed : CommitResult

    /**
     * Nothing differed from the last commit.
     */
    public data object NothingToCommit : CommitResult

    /**
     * A merge left files in conflict, so nothing was committed.
     */
    public data object Conflicts : CommitResult

    /**
     * The repository couldn't be read or written, or is gone.
     */
    public data object Failed : CommitResult
}

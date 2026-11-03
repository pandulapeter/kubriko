/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace
import com.pandulapeter.kubrikoCreator.git.GitRepository
import com.pandulapeter.kubrikoCreator.git.JGitRepository
import java.io.File

/**
 * Opens the project's repository, or makes the project one.
 *
 * @param open The project's repository, or null if it has none.
 * @param create Makes the project a repository, committing everything with the given message.
 */
public class GitRepositories(
    internal val open: () -> GitRepository?,
    internal val create: (message: String) -> GitRepository,
) {
    public companion object {
        /**
         * The repository at [rootDirectory], through JGit.
         */
        public fun of(rootDirectory: File): GitRepositories = GitRepositories(
            open = { JGitRepository.open(rootDirectory) },
            create = { JGitRepository.create(rootDirectory, it) },
        )
    }
}

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

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.File

/**
 * A git repository on disk, through JGit, so that nothing has to be installed and every platform behaves the same.
 * Commits use the author configured for git (`user.name` and `user.email`), as the git command line would.
 */
internal class JGitRepository private constructor(
    private val rootDirectory: File,
) : GitRepository {

    override fun status() = git { git ->
        val status = git.status().call()
        val changes = buildList {
            (status.untracked + status.added).forEach { add(FileChange(it, ChangeKind.ADDED)) }
            (status.modified + status.changed).forEach { add(FileChange(it, ChangeKind.MODIFIED)) }
            (status.missing + status.removed).forEach { add(FileChange(it, ChangeKind.DELETED)) }
            status.conflicting.forEach { add(FileChange(it, ChangeKind.CONFLICTING)) }
        }
        RepositoryStatus(
            branch = git.repository.fullBranch?.takeIf { it.startsWith(Constants.R_HEADS) }?.removePrefix(Constants.R_HEADS),
            changes = changes.distinctBy { it.path }.sortedBy { it.path },
        )
    }

    override fun commitAll(message: String) = git { git ->
        git.add().addFilepattern(".").call()
        git.add().setUpdate(true).addFilepattern(".").call()
        git.commit().setMessage(message).call()
        Unit
    }

    override fun history(limit: Int) = git { git ->
        try {
            git.log().setMaxCount(limit).call().map { commit ->
                CommitSummary(
                    id = commit.id.abbreviate(7).name(),
                    message = commit.shortMessage,
                    author = commit.authorIdent.name,
                    time = commit.authorIdent.whenAsInstant.toEpochMilli(),
                )
            }
        } catch (_: NoHeadException) {
            emptyList()
        }
    }

    override fun discard(paths: Collection<String>) = git { git ->
        val head = git.repository.resolve(Constants.HEAD)
        val headTree = head?.let { git.repository.parseCommit(it).tree }
        val committed = paths.filter { path -> headTree != null && TreeWalk.forPath(git.repository, path, headTree) != null }
        val uncommitted = paths - committed.toSet()
        if (uncommitted.isNotEmpty() && head != null) {
            git.reset().apply { uncommitted.forEach(::addPath) }.call()
        }
        uncommitted.forEach { File(rootDirectory, it).delete() }
        if (committed.isNotEmpty()) {
            git.reset().apply { committed.forEach(::addPath) }.call()
            git.checkout().setStartPoint(Constants.HEAD).apply { committed.forEach(::addPath) }.call()
        }
    }

    private fun <T> git(action: (Git) -> T) = Git.open(rootDirectory).use(action)

    companion object {

        /**
         * The repository [directory] is the root of, or null if it isn't the root of one (a project inside a larger
         * repository is left to other tools).
         */
        fun open(directory: File) = if (File(directory, Constants.DOT_GIT).exists()) JGitRepository(directory) else null

        /**
         * Makes [directory] a repository on a `main` branch, with everything in it committed with [message].
         */
        fun create(directory: File, message: String): JGitRepository {
            Git.init().setDirectory(directory).setInitialBranch("main").call().close()
            return JGitRepository(directory).apply { commitAll(message) }
        }
    }
}

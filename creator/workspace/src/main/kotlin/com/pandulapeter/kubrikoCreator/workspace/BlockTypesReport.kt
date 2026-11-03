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

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypesListener
import com.pandulapeter.kubrikoCreator.actors.store.FailedWrite
import com.pandulapeter.kubrikoCreator.actors.store.UnreadableBlockTypeFile
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.context.KotlinBlockScopeChecks
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFileChanges
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Says in the [console] what the project's type store did, and hands each saved batch to [onBatchSaved].
 */
internal class BlockTypesReport(
    private val console: ConsoleLog,
    private val onBatchSaved: (haveTypesChanged: Boolean) -> Unit,
) : BlockTypesListener {
    private val hasReportedNewerKubrikoVersion = AtomicBoolean(false)
    private val hasReportedUnavailableKubrikoVersion = AtomicBoolean(false)

    override fun onSaved(haveTypesChanged: Boolean) = onBatchSaved(haveTypesChanged)

    override fun onSaveFailed(failure: FailedWrite) = console.append(failure.toConsoleEntry())

    override fun onSavedAgain() = console.append(ConsoleEntry.Status(WorkspaceStatus.BLOCK_TYPES_SAVED_AGAIN))

    override fun onFileUnreadable(file: UnreadableBlockTypeFile) = console.append(file.toConsoleEntry())

    override fun onFilesReadableAgain() = console.append(ConsoleEntry.Status(WorkspaceStatus.TYPE_FILES_READABLE_AGAIN))

    override fun onLeftoversRemoved(paths: List<String>) =
        console.append(ConsoleEntry.Status(WorkspaceStatus.GENERATED_LEFTOVERS_REMOVED, arguments = listOf(paths.joinToString())))

    override fun onLeftoverChangedByHand(path: String) =
        console.append(ConsoleEntry.Status(WorkspaceStatus.GENERATED_LEFTOVER_CHANGED_BY_HAND, isError = true, arguments = listOf(path)))

    /**
     * Names, once per type, the types whose Kotlin Blocks rely on what changed when each kind of Event got a function of
     * its own, which the health checks list Block by Block.
     */
    override fun onFirstLoaded(types: List<BlockTypeDefinition>) {
        KotlinBlockScopeChecks.of(types).map { it.subject.toString() }.distinct().forEach { name ->
            console.append(ConsoleEntry.Status(WorkspaceStatus.KOTLIN_BLOCK_SCOPE_CHANGED, isError = true, arguments = listOf(name)))
        }
    }

    override fun onBuildFilesSynced(changes: KubrikoBuildFileChanges) {
        changes.previousVersion?.let { console.append(ConsoleEntry.Status(WorkspaceStatus.KUBRIKO_VERSION_CHANGED, arguments = listOf(changes.version, it))) }
        if (changes.isNewerThanCreator && !hasReportedNewerKubrikoVersion.getAndSet(true)) {
            console.append(ConsoleEntry.Status(WorkspaceStatus.KUBRIKO_VERSION_NEWER, arguments = listOf(changes.version)))
        }
        changes.unavailableVersion?.takeIf { !hasReportedUnavailableKubrikoVersion.getAndSet(true) }?.let { unavailable ->
            console.append(ConsoleEntry.Status(WorkspaceStatus.KUBRIKO_VERSION_UNAVAILABLE, isError = true, arguments = listOf(unavailable, changes.version)))
        }
        if (changes.addedArtifacts.isNotEmpty()) {
            console.append(ConsoleEntry.Status(WorkspaceStatus.KUBRIKO_LIBRARIES_ADDED, arguments = listOf(changes.addedArtifacts.joinToString())))
        }
        if (changes.removedArtifacts.isNotEmpty()) {
            console.append(ConsoleEntry.Status(WorkspaceStatus.KUBRIKO_LIBRARIES_REMOVED, arguments = listOf(changes.removedArtifacts.joinToString())))
        }
    }
}

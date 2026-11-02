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

import com.pandulapeter.kubrikoCreator.project.ProjectChange
import com.pandulapeter.kubrikoCreator.project.ProjectSnapshot
import com.pandulapeter.kubrikoCreator.scenes.SceneChange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

/**
 * The one table routing every change to the project, Creator's own writes ([onSceneChanges], [onTypesSaved]) and the
 * watcher's ([onProjectChanged]), to the parts that follow it. Used from the main thread only.
 *
 * A Scene edit refreshes only the Scene view and what is built from the Scenes; a new, deleted, or moved Scene or a folder
 * the resource tree too; and every change the Version control panel. Resource refreshes are coalesced: at most one scan
 * runs and one waits, however many changes arrive meanwhile. After a change the watcher found, the tree is built from the
 * snapshot it found it in, without walking the project again, unless one of Creator's own writes, which that snapshot may
 * predate, is waiting for a scan too.
 *
 * @param scanResources Scans the resource tree, from the given snapshot or, for null, by walking the project, and
 *   publishes it, run by one loop in [scope].
 * @param onScenesChanged Tells the Scene view which Scenes changed.
 * @param onSceneRenamed Tells the Scene view that a Scene moved to another name.
 * @param onScenesChangedOnDisk Tells the Scene view that Scenes changed outside Kubriko Creator.
 * @param markSceneOutdated Tells the Scene view that the code it was loaded from changed.
 * @param regenerate Regenerates the code of the block-built types (cheap when nothing it depends on changed).
 * @param onCodeChanged Tells the playing game that the project's code changed.
 */
internal class ProjectChanges(
    scope: CoroutineScope,
    private val scanResources: suspend (snapshot: ProjectSnapshot?) -> Unit,
    private val onScenesChanged: (names: Collection<String>) -> Unit,
    private val onSceneRenamed: (oldName: String, newName: String) -> Unit,
    private val onScenesChangedOnDisk: () -> Unit,
    private val markSceneOutdated: () -> Unit,
    private val regenerate: () -> Unit,
    private val onCodeChanged: () -> Unit,
    private val refreshGit: () -> Unit,
    private val refreshFormatStatus: () -> Unit,
    private val reloadTypes: () -> Unit,
    private val refreshProjectUpgrade: () -> Unit,
) {
    private val resourceRefreshes = Channel<ResourceRefresh>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (first in resourceRefreshes) {
                var snapshot = first.snapshot
                var isWalkNeeded = snapshot == null
                while (true) {
                    val next = resourceRefreshes.tryReceive().getOrNull() ?: break
                    if (next.snapshot == null) isWalkNeeded = true else snapshot = next.snapshot
                }
                try {
                    scanResources(if (isWalkNeeded) null else snapshot)
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    currentCoroutineContext()[CoroutineExceptionHandler]?.handleException(currentCoroutineContext(), exception)
                }
            }
        }
    }

    /**
     * Scans the resource tree again by walking the project, once the scan running (if any) finishes.
     */
    fun refreshResources() {
        resourceRefreshes.trySend(ResourceRefresh(snapshot = null))
    }

    /**
     * Follows what one command, undo, or redo changed in the Scene files.
     */
    fun onSceneChanges(changes: List<SceneChange>) {
        if (changes.any { it !is SceneChange.Written || it.isNew }) refreshResources()
        changes.forEach { if (it is SceneChange.Moved) onSceneRenamed(it.oldName, it.newName) }
        onScenesChanged(changes.flatMapTo(LinkedHashSet()) { it.names() })
        regenerate()
        onCodeChanged()
        refreshGit()
    }

    /**
     * Follows a batch of block type writes, which changed the types themselves when [haveTypesChanged] (and only their
     * generated code otherwise, which the resource tree leaves out).
     */
    fun onTypesSaved(haveTypesChanged: Boolean) {
        if (haveTypesChanged) {
            markSceneOutdated()
            onCodeChanged()
        }
        refreshGit()
    }

    /**
     * Follows files that changed on disk outside Kubriko Creator, which [snapshot] (if known) shows as they are now.
     */
    fun onProjectChanged(changes: Set<ProjectChange>, snapshot: ProjectSnapshot? = null) {
        if (ProjectChange.SETTINGS in changes) refreshFormatStatus()
        if (ProjectChange.BLOCK_TYPE_DEFINITIONS in changes) reloadTypes()
        if (ProjectChange.SOURCES in changes) {
            markSceneOutdated()
            refreshProjectUpgrade()
        }
        if (ProjectChange.SCENES in changes) {
            regenerate()
            onScenesChangedOnDisk()
        }
        if (ProjectChange.SCENES in changes || ProjectChange.SOURCES in changes) {
            resourceRefreshes.trySend(ResourceRefresh(snapshot))
            onCodeChanged()
        }
        refreshGit()
    }

    /**
     * A request for a scan, from [snapshot] or, for null, by walking the project.
     */
    private class ResourceRefresh(val snapshot: ProjectSnapshot?)

    private fun SceneChange.names() = when (this) {
        is SceneChange.Written -> listOf(name)
        is SceneChange.Deleted -> listOf(name)
        is SceneChange.Moved -> listOf(oldName, newName)
        SceneChange.FoldersChanged -> emptyList()
    }
}

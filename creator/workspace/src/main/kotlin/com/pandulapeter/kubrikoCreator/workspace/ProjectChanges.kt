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

import com.pandulapeter.kubrikoCreator.foundation.reportProblem
import com.pandulapeter.kubrikoCreator.project.ProjectChange
import com.pandulapeter.kubrikoCreator.project.ProjectSnapshot
import com.pandulapeter.kubrikoCreator.scenes.SceneChange
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The one table routing every change to the project, Creator's own writes ([onSceneChanges], [onTypesSaved]) and the
 * watcher's ([onProjectChanged]), to the parts that follow it, each of which follows it whatever another one throws.
 * Used from the main thread only.
 *
 * A Scene edit refreshes only the Scene view and what is built from the Scenes; a new, deleted, or moved Scene or a folder
 * the resource tree too; and every change the Version control panel. Resource refreshes are coalesced: at most one scan
 * runs and one waits, however many changes arrive meanwhile. After a change the watcher found, the tree is built from the
 * snapshot it found it in, without walking the project again, unless one of Creator's own writes, which that snapshot may
 * predate, is waiting for a scan too ([scanResources] walks for a snapshot taken before an own write that already ran).
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
    private val scope: CoroutineScope,
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
        checkNotNull(scope.coroutineContext[CoroutineExceptionHandler]) { "ProjectChanges reports what fails through its scope's handler, which it lacks." }
        scope.launch {
            for (first in resourceRefreshes) {
                // A refresh queued before the scope was cancelled is received without a check, so no scan starts after it.
                currentCoroutineContext().ensureActive()
                var snapshot = first.snapshot
                var isWalkNeeded = snapshot == null
                while (true) {
                    val next = resourceRefreshes.tryReceive().getOrNull() ?: break
                    if (next.snapshot == null) isWalkNeeded = true else snapshot = next.snapshot
                }
                try {
                    scanResources(if (isWalkNeeded) null else snapshot)
                } catch (throwable: Throwable) {
                    // A scan failing while the workspace closes is no problem of a project still open.
                    if (scope.isActive) scope.reportProblem(throwable)
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
     * Follows what one command, undo, or redo changed in the Scene files, which it is handed only once.
     */
    fun onSceneChanges(changes: List<SceneChange>) {
        following { if (changes.any { it !is SceneChange.Written || it.isNew }) refreshResources() }
        changes.forEach { if (it is SceneChange.Moved) following { onSceneRenamed(it.oldName, it.newName) } }
        following { onScenesChanged(changes.flatMapTo(LinkedHashSet()) { it.names() }) }
        following(regenerate)
        following(onCodeChanged)
        following(refreshGit)
    }

    /**
     * Runs [follower], one part that follows a change, reporting what it throws ([reportProblem]), so the parts after it
     * still follow the change. Once the scope is cancelled (the workspace is closing), nothing follows changes any more.
     */
    private inline fun following(follower: () -> Unit) {
        if (!scope.isActive) return
        try {
            follower()
        } catch (throwable: Throwable) {
            scope.reportProblem(throwable)
        }
    }

    /**
     * Follows Kubriko Creator's own writes of sprites' images, which the watcher never reports, as each raises [revision]:
     * as for an image changed outside it, the Scene view is built again and the playing game follows, at most once every
     * [SPRITE_IMAGES_SETTLING_IN_MILLISECONDS] while writes keep coming (strokes drawn one after the other), so a long run
     * of them still reaches both within that time.
     */
    fun followSpriteImageWrites(revision: Flow<Int>) {
        scope.launch {
            revision.drop(1).sample(SPRITE_IMAGES_SETTLING_IN_MILLISECONDS).collect {
                following(markSceneOutdated)
                following(onCodeChanged)
            }
        }
    }

    private val spriteSheetsLock = Any()

    /**
     * How the sprites are cut into frames as last followed, or null before the first scan read them.
     */
    private var knownSpriteSheets: Map<String, SpriteSheet>? = null

    /**
     * Takes the sheets the first scan read ([scanned]) as what later ones are compared with, since the opening generates
     * the code from them anyway, then follows the sheets the sprites' store has [now], which a change made while the scan
     * ran may already have moved on from, and which the store's own updates may have reported before there was anything
     * to compare them with. Only the first scan's call sets what is compared with. Called on the main thread, once the
     * scan's resources are published, as every follower runs.
     */
    fun onSpriteSheetsScanned(scanned: Map<String, SpriteSheet>, now: Map<String, SpriteSheet>) {
        synchronized(spriteSheetsLock) { if (knownSpriteSheets == null) knownSpriteSheets = scanned }
        onSpriteSheets(now)
    }

    /**
     * Follows how the sprites are cut into frames as the sprites' store has them now, whoever changed them: once they
     * differ from what was last followed, the types drawn as them are generated again, so the Scene view and the playing
     * game follow their code. Nothing is followed before the first scan.
     */
    fun onSpriteSheets(sheets: Map<String, SpriteSheet>) {
        val hasChanged = synchronized(spriteSheetsLock) {
            val known = knownSpriteSheets
            (known != null && known != sheets).also { if (it) knownSpriteSheets = sheets }
        }
        if (hasChanged) {
            following(regenerate)
            following(markSceneOutdated)
            following(onCodeChanged)
        }
    }

    /**
     * Follows a batch of block type writes, which changed the types themselves when [haveTypesChanged] (and only their
     * generated code otherwise, which the resource tree leaves out).
     */
    fun onTypesSaved(haveTypesChanged: Boolean) {
        if (haveTypesChanged) {
            following(markSceneOutdated)
            following(onCodeChanged)
        }
        following(refreshGit)
    }

    /**
     * Follows files that changed on disk outside Kubriko Creator, which [snapshot] (if known) shows as they are now.
     */
    fun onProjectChanged(changes: Set<ProjectChange>, snapshot: ProjectSnapshot? = null) {
        if (ProjectChange.SETTINGS in changes) following(refreshFormatStatus)
        if (ProjectChange.BLOCK_TYPE_DEFINITIONS in changes) following(reloadTypes)
        if (ProjectChange.SOURCES in changes) {
            following(markSceneOutdated)
            following(refreshProjectUpgrade)
        }
        if (ProjectChange.SCENES in changes) {
            following(regenerate)
            following(onScenesChangedOnDisk)
        }
        if (ProjectChange.SCENES in changes || ProjectChange.SOURCES in changes) {
            resourceRefreshes.trySend(ResourceRefresh(snapshot))
            following(onCodeChanged)
        } else if (ProjectChange.MODULES in changes || ProjectChange.DOCUMENTS in changes || ProjectChange.SPRITE_FOLDERS in changes) {
            resourceRefreshes.trySend(ResourceRefresh(snapshot))
        }
        following(refreshGit)
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

/**
 * How often at most Kubriko Creator's own writes of sprites' images are followed ([ProjectChanges.followSpriteImageWrites]),
 * so a burst of strokes rebuilds the Scene view and reaches the game once a second rather than once a stroke.
 */
internal const val SPRITE_IMAGES_SETTLING_IN_MILLISECONDS = 1_000L

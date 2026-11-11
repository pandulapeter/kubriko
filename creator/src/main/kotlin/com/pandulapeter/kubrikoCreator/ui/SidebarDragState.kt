/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup

/**
 * An Actor type, Manager type, Scene, page, sprite, sound, music, document, or folder being dragged in the sidebar, in the
 * window's root coordinates: into a folder of its group (or onto the group's header, out of every folder; a folder never
 * into itself or one inside it), a document into a document folder or a band of any module, or for an Actor type, into
 * the open Scene, which registers itself as [sceneTarget].
 */
@Stable
internal class SidebarDragState(
    private val clock: () -> Long = System::nanoTime,
) {
    private var lastDragEndedAt = Long.MIN_VALUE / 2

    /**
     * @param name Its name, or for a document, its path from the project's root.
     * @param group Its group, or null for a document.
     * @param folder The folder it is in now, for a document from the project's root.
     * @param module The module its group is shown in, which it can't be moved out of (unlike a document).
     * @param isFolder Whether it is a folder of [group], by its path in [name], moving with everything in it.
     */
    data class Dragged(
        val name: String,
        val group: FolderGroup?,
        val folder: String,
        val module: String = "",
        val isFolder: Boolean = false,
    )

    /**
     * A folder row of the sidebar, or a group's header for an empty [path], in the group of [module]; for a null [group],
     * a document folder or a band, by its [path] from the project's root.
     */
    data class FolderKey(
        val group: FolderGroup?,
        val path: String,
        val module: String = "",
    )

    /**
     * Where Actor types can be placed, and what happens to one dropped at a point (in root coordinates) inside it.
     */
    class SceneTarget(
        val bounds: Rect,
        val onDrop: (actorTypeName: String, at: Offset) -> Unit,
    )

    var dragged by mutableStateOf<Dragged?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    var sceneTarget: SceneTarget? = null

    /**
     * Where something dragged would move to a folder if dropped: a folder row or group header (moving into that folder, or
     * out of every folder), or an item's row (moving next to it, into its folder). Kept up to date by the rows themselves,
     * by an ID of their own.
     */
    class DropTarget(
        val folder: FolderKey,
        val bounds: Rect,
    )

    val dropTargets = HashMap<Any, DropTarget>()

    /**
     * Moves the type, Scene, or page called [name] of [group], or the document at [name] for a null group, to [folder].
     */
    var onMoveToFolder: (group: FolderGroup?, name: String, folder: String) -> Unit = { _, _, _ -> }

    /**
     * Moves the folder at [path] of [group], with everything in it, into [folder].
     */
    var onMoveFolder: (group: FolderGroup, path: String, folder: String) -> Unit = { _, _, _ -> }

    val isOverScene get() = dragged?.let { it.group == FolderGroup.ACTOR_TYPES && !it.isFolder } == true && sceneTarget?.bounds?.contains(pointer) == true

    /**
     * The folder what is dragged would move to if it were dropped now.
     */
    val hoveredFolder: FolderKey?
        get() {
            val dragged = dragged ?: return null
            return dropTargets.values.firstOrNull { target ->
                target.folder.group == dragged.group &&
                    (dragged.group == null || target.folder.module == dragged.module) &&
                    !(dragged.isFolder && FolderPaths.isWithin(target.folder.path, dragged.name)) &&
                    target.bounds.contains(pointer)
            }?.folder
        }

    fun start(dragged: Dragged, at: Offset) {
        this.dragged = dragged
        pointer = at
    }

    fun moveTo(at: Offset) {
        pointer = at
    }

    fun end() {
        val dragged = dragged ?: return
        val scene = sceneTarget?.takeIf { isOverScene }
        val folder = hoveredFolder?.path
        cancel()
        when {
            scene != null -> scene.onDrop(dragged.name, pointer)
            folder == null || folder == dragged.folder -> Unit
            dragged.group != null && dragged.isFolder -> onMoveFolder(dragged.group, dragged.name, folder)
            else -> onMoveToFolder(dragged.group, dragged.name, folder)
        }
    }

    fun cancel() {
        dragged = null
        lastDragEndedAt = clock()
    }

    /**
     * Whether a click now is the release of a drag that just ended, which the dragged row's own click ignores.
     */
    val isClickAfterDrag get() = clock() - lastDragEndedAt < CLICK_AFTER_DRAG_IN_NANOSECONDS

    private companion object {
        const val CLICK_AFTER_DRAG_IN_NANOSECONDS = 300_000_000L
    }
}

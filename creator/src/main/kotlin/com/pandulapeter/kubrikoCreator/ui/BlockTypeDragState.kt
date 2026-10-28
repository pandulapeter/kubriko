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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind

/**
 * An Actor type or Manager type being dragged from the sidebar, in the window's root coordinates: into a folder of its
 * group (or onto the group's header, out of every folder), or for an Actor type, into the open Scene, which registers
 * itself as [sceneTarget].
 */
@Stable
internal class BlockTypeDragState(
    private val clock: () -> Long = System::nanoTime,
) {
    private var lastDragEndedAt = Long.MIN_VALUE / 2

    /**
     * @param folder The folder it is in now.
     */
    data class Dragged(
        val name: String,
        val kind: BlockTypeKind,
        val folder: String,
    )

    /**
     * A folder row of the sidebar, or a group's header for an empty [path].
     */
    data class FolderKey(
        val kind: BlockTypeKind,
        val path: String,
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
     * Where the sidebar's folder rows and group headers are, kept up to date by the rows themselves.
     */
    val folderTargets = HashMap<FolderKey, Rect>()

    /**
     * Moves the type called [name] to [folder].
     */
    var onMoveToFolder: (name: String, folder: String) -> Unit = { _, _ -> }

    val isOverScene get() = dragged?.kind == BlockTypeKind.ACTOR && sceneTarget?.bounds?.contains(pointer) == true

    /**
     * The folder the dragged type would move to if it were dropped now.
     */
    val hoveredFolder: FolderKey?
        get() {
            val dragged = dragged ?: return null
            return folderTargets.entries.firstOrNull { (key, bounds) -> key.kind == dragged.kind && bounds.contains(pointer) }?.key
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
            folder != null && folder != dragged.folder -> onMoveToFolder(dragged.name, folder)
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

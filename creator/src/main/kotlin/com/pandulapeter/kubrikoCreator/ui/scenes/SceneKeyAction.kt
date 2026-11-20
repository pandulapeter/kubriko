/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.ui.input.key.Key

/**
 * What a key pressed on the Scene canvas does to the selection.
 */
internal sealed interface SceneKeyAction {
    data object Remove : SceneKeyAction

    data object Duplicate : SceneKeyAction

    data object SelectAll : SceneKeyAction

    data object ClearSelection : SceneKeyAction

    /**
     * Moves the selected Actors by [dx] and [dy] scene units.
     */
    data class Nudge(val dx: Float, val dy: Float) : SceneKeyAction

    companion object {
        /**
         * What [key] does, with Shift ([isShiftPressed]) and Ctrl or Cmd ([isCommandPressed]) held or not: Delete and
         * Backspace remove, Ctrl or Cmd with D duplicates and with A selects every Actor, Escape selects none, and the
         * arrows move by [step], ten times as far with Shift.
         */
        fun of(key: Key, isShiftPressed: Boolean, isCommandPressed: Boolean, step: Float): SceneKeyAction? {
            val distance = if (isShiftPressed) step * SHIFT_NUDGE_FACTOR else step
            return when {
                key == Key.Delete || key == Key.Backspace -> Remove
                isCommandPressed && key == Key.D -> Duplicate
                isCommandPressed && key == Key.A -> SelectAll
                isCommandPressed -> null
                key == Key.Escape -> ClearSelection
                key == Key.DirectionLeft -> Nudge(-distance, 0f)
                key == Key.DirectionRight -> Nudge(distance, 0f)
                key == Key.DirectionUp -> Nudge(0f, -distance)
                key == Key.DirectionDown -> Nudge(0f, distance)
                else -> null
            }
        }

        private const val SHIFT_NUDGE_FACTOR = 10
    }
}

/**
 * Does what [action] says to the selected Actors [selectedIds], whether it did anything: one with nothing to act on (no
 * selection, nothing to select, nothing that moves) leaves the key to the elements around the canvas.
 *
 * @param drawnIds The Actors selecting all selects.
 * @param nudge Moves the selected Actors, whether it moved any.
 */
internal fun performSceneKeyAction(
    action: SceneKeyAction,
    selectedIds: List<String>,
    drawnIds: () -> List<String>,
    onRemove: (List<String>) -> Unit,
    onDuplicate: (List<String>) -> Unit,
    onSelectionChange: (List<String>) -> Unit,
    nudge: (dx: Float, dy: Float) -> Boolean,
): Boolean = when (action) {
    SceneKeyAction.Remove -> selectedIds.isNotEmpty().also { if (it) onRemove(selectedIds) }
    SceneKeyAction.Duplicate -> selectedIds.isNotEmpty().also { if (it) onDuplicate(selectedIds) }
    SceneKeyAction.ClearSelection -> selectedIds.isNotEmpty().also { if (it) onSelectionChange(emptyList()) }
    SceneKeyAction.SelectAll -> drawnIds().let { ids -> ids.isNotEmpty().also { if (it) onSelectionChange(ids) } }
    is SceneKeyAction.Nudge -> nudge(action.dx, action.dy)
}

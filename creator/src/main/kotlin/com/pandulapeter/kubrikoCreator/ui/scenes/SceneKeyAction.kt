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

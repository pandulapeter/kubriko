/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

/**
 * How the Scene editor gets from one version of a Scene to the next without creating every Actor again: for each entry
 * of the new version, in order, what happens to the Actor shown for it ([actions]), and which shown Actors go
 * ([removedIndices], indices in the old version).
 */
internal class ScenePatch(
    val actions: List<Action>,
    val removedIndices: List<Int>,
) {
    sealed interface Action {

        /**
         * The Actor shown for the old entry at [previousIndex] stays as it is.
         */
        data class Keep(val previousIndex: Int) : Action

        /**
         * The Actor shown for the old entry at [previousIndex] only moves to [x], [y].
         */
        data class Move(val previousIndex: Int, val x: Float, val y: Float) : Action

        /**
         * The Actor shown for the old entry at [previousIndex] is created again from the new state.
         */
        data class Replace(val previousIndex: Int) : Action

        /**
         * A new Actor is created for the entry.
         */
        data object Add : Action
    }

    companion object {

        /**
         * The patch from [previous] to [next], or null if the Actors that stay changed their order, or new ones came
         * before old ones, which only creating the Scene again keeps in the order Kubriko draws and picks them in.
         *
         * @param isRecreated Whether the Actor of an entry that stays is created again even if its state didn't change,
         *   because what it is created as did.
         */
        fun between(
            previous: List<SceneActorEntry>,
            next: List<SceneActorEntry>,
            isRecreated: (SceneActorEntry) -> Boolean = { false },
        ): ScenePatch? {
            val previousIndices = HashMap<String, Int>(previous.size * 2)
            previous.forEachIndexed { index, entry -> previousIndices[entry.id] = index }
            var lastPreviousIndex = -1
            var hasAdded = false
            val actions = next.map { entry ->
                val previousIndex = previousIndices[entry.id]
                if (previousIndex == null) {
                    hasAdded = true
                    return@map Action.Add
                }
                if (hasAdded || previousIndex < lastPreviousIndex) return null
                lastPreviousIndex = previousIndex
                val old = previous[previousIndex]
                when {
                    isRecreated(entry) -> Action.Replace(previousIndex)
                    old == entry -> Action.Keep(previousIndex)
                    old.typeId == entry.typeId && SceneDocument.isOnlyMoved(old.state, entry.state) ->
                        SceneDocument.positionOf(entry.state)?.let { (x, y) -> Action.Move(previousIndex, x, y) } ?: Action.Replace(previousIndex)
                    else -> Action.Replace(previousIndex)
                }
            }
            val nextIds = next.mapTo(HashSet(next.size * 2)) { it.id }
            return ScenePatch(actions, previous.indices.filter { previous[it].id !in nextIds })
        }
    }
}

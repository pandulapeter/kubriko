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
public class ScenePatch(
    public val actions: List<Action>,
    public val removedIndices: List<Int>,
) {
    /**
     * What happens to the Actor shown for one entry of the new version.
     */
    public sealed interface Action {

        /**
         * The Actor shown for the old entry at [previousIndex] stays as it is.
         */
        public data class Keep(val previousIndex: Int) : Action

        /**
         * The Actor shown for the old entry at [previousIndex] only moves to [x], [y].
         */
        public data class Move(val previousIndex: Int, val x: Float, val y: Float) : Action

        /**
         * The Actor shown for the old entry at [previousIndex] is created again from the new state.
         */
        public data class Replace(val previousIndex: Int) : Action

        /**
         * A new Actor is created for the entry.
         */
        public data object Add : Action
    }

    public companion object {

        /**
         * The patch from [previous] to [next]. Kubriko draws Actors of the same drawing order in the order they were
         * added, and new ones always go last, so where the order of the Actors that stay changes, or new ones come
         * before old ones, the Actors from there on are created again ([orderedTailStart]), and the ones before it stay.
         *
         * @param isRecreated Whether the Actor of an entry that stays is created again even if its state didn't change,
         *   because what it is created as did.
         */
        public fun between(
            previous: List<SceneActorEntry>,
            next: List<SceneActorEntry>,
            isRecreated: (SceneActorEntry) -> Boolean = { false },
        ): ScenePatch {
            val previousIndices = HashMap<String, Int>(previous.size * 2)
            previous.forEachIndexed { index, entry -> previousIndices[entry.id] = index }
            val tailStart = orderedTailStart(next) { previousIndices[it.id] }
            val actions = next.mapIndexed { index, entry ->
                val previousIndex = previousIndices[entry.id] ?: return@mapIndexed Action.Add
                val old = previous[previousIndex]
                when {
                    index >= tailStart || isRecreated(entry) -> Action.Replace(previousIndex)
                    old == entry -> Action.Keep(previousIndex)
                    old.typeId == entry.typeId && SceneDocument.isOnlyMoved(old.state, entry.state) ->
                        SceneDocument.positionOf(entry.state)?.let { (x, y) -> Action.Move(previousIndex, x, y) } ?: Action.Replace(previousIndex)
                    else -> Action.Replace(previousIndex)
                }
            }
            val nextIds = next.mapTo(HashSet(next.size * 2)) { it.id }
            return ScenePatch(actions, previous.indices.filter { previous[it].id !in nextIds })
        }

        /**
         * The index of [next] from which its Actors are created again so that they end up in its order: the first new
         * entry or the first one out of order ([previousIndexOf] smaller than one before it), whichever comes first,
         * if an Actor that stays comes after it; [next]'s size if none does.
         */
        private fun orderedTailStart(next: List<SceneActorEntry>, previousIndexOf: (SceneActorEntry) -> Int?): Int {
            var lastPreviousIndex = -1
            var firstAdded = -1
            next.forEachIndexed { index, entry ->
                val previousIndex = previousIndexOf(entry)
                when {
                    previousIndex == null -> if (firstAdded < 0) firstAdded = index
                    firstAdded >= 0 -> return firstAdded
                    previousIndex < lastPreviousIndex -> return index
                    else -> lastPreviousIndex = previousIndex
                }
            }
            return next.size
        }
    }
}

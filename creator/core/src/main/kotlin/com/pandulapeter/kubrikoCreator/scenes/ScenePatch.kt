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
     * Whether an entry is new, or at another index than before, so that what is kept by index (such as where each
     * Actor can be picked) has to be built again rather than updated.
     */
    public val hasNewIndices: Boolean = actions.withIndex().any { (index, action) ->
        when (action) {
            Action.Add -> true
            is Action.Kept -> action.previousIndex != index
            is Action.Replace -> action.previousIndex != index
        }
    }

    /**
     * What happens to the Actor shown for one entry of the new version.
     */
    public sealed interface Action {

        /**
         * An action whose Actor stays the one shown for the old entry at [previousIndex].
         */
        public sealed interface Kept : Action {
            public val previousIndex: Int
        }

        /**
         * The Actor shown for the old entry at [previousIndex] stays as it is.
         */
        public data class Keep(override val previousIndex: Int) : Kept

        /**
         * The Actor shown for the old entry at [previousIndex] only moves to [x], [y].
         */
        public data class Move(override val previousIndex: Int, val x: Float, val y: Float) : Kept

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
         * added, and created ones always go last, so a changed Actor is drawn after the others for a while. Where the
         * order of the Actors that stay changes, or new ones come before them, the Actors from there on are created
         * again ([orderedTailStart]), and the ones before it stay, so the Scene is drawn in its order again.
         *
         * @param previousDrawnRanks Where each of [previous]'s Actors is in the order Kubriko draws them (any increasing
         *   numbers), or null if that is [previous]'s own order.
         * @param isRecreated Whether the Actor of an entry that stays is created again even if its state didn't change,
         *   because what it is created as did.
         */
        public fun between(
            previous: List<SceneActorEntry>,
            next: List<SceneActorEntry>,
            previousDrawnRanks: IntArray? = null,
            isRecreated: (SceneActorEntry) -> Boolean = { false },
        ): ScenePatch {
            require(previousDrawnRanks == null || previousDrawnRanks.size == previous.size) { "One drawn rank is needed for each previous entry." }
            val previousIndices = HashMap<String, Int>(previous.size * 2)
            previous.forEachIndexed { index, entry -> previousIndices[entry.id] = index }
            val actions = next.map { entry ->
                val previousIndex = previousIndices[entry.id] ?: return@map Action.Add
                val old = previous[previousIndex]
                when {
                    isRecreated(entry) -> Action.Replace(previousIndex)
                    old == entry -> Action.Keep(previousIndex)
                    old.typeId == entry.typeId && SceneDocument.isOnlyMoved(old.state, entry.state) ->
                        SceneDocument.positionOf(entry.state)?.let { (x, y) -> Action.Move(previousIndex, x, y) } ?: Action.Replace(previousIndex)
                    else -> Action.Replace(previousIndex)
                }
            }
            val tailStart = if (previousDrawnRanks == null) orderedTailStart(actions) { it } else orderedTailStart(actions) { previousDrawnRanks[it] }
            val nextIds = next.mapTo(HashSet(next.size * 2)) { it.id }
            return ScenePatch(
                actions = actions.mapIndexed { index, action -> if (index >= tailStart && action is Action.Kept) Action.Replace(action.previousIndex) else action },
                removedIndices = previous.indices.filter { previous[it].id !in nextIds },
            )
        }

        /**
         * The index of [actions] from which the Actors are created again so that they end up drawn in their new order.
         * Only a new order (an Actor that stays coming before one it came after, or after a new one) needs it, and then
         * it starts at that Actor, or earlier at the first new or changed one, or at the first one drawn out of its order
         * ([drawnRankOf] its previous index smaller than one before it), so that what stays is drawn in order and what
         * is created goes after it. A changed Actor's old place counts toward the new order like any other. Without a
         * new order, it is [actions]' size.
         */
        private inline fun orderedTailStart(actions: List<Action>, drawnRankOf: (Int) -> Int): Int {
            var lastPreviousIndex = -1
            var lastRank = Int.MIN_VALUE
            var firstAdded = -1
            var firstUnsafe = -1
            actions.forEachIndexed { index, action ->
                val previousIndex = when (action) {
                    is Action.Kept -> action.previousIndex
                    is Action.Replace -> action.previousIndex
                    Action.Add -> -1
                }
                if (previousIndex >= 0 && (firstAdded >= 0 || previousIndex < lastPreviousIndex)) {
                    return if (firstUnsafe >= 0) firstUnsafe else index
                }
                when (action) {
                    is Action.Kept -> {
                        val rank = drawnRankOf(previousIndex)
                        if (rank < lastRank) {
                            if (firstUnsafe < 0) firstUnsafe = index
                        } else {
                            lastRank = rank
                        }
                    }
                    Action.Add -> if (firstAdded < 0) firstAdded = index
                    is Action.Replace -> Unit
                }
                if (action !is Action.Kept && firstUnsafe < 0) firstUnsafe = index
                if (previousIndex >= 0) lastPreviousIndex = previousIndex
            }
            return actions.size
        }
    }
}

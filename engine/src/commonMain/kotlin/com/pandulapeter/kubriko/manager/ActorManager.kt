/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

/**
 * Responsible for managing the lifecycle of [Actor]s in the game world.
 * This includes adding, removing, and providing access to actors based on their traits (e.g., visibility, dynamics).
 */
sealed class ActorManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "ActorManager",
) {
    /**
     * All [Actor]s currently registered in the engine.
     */
    abstract val allActors: StateFlow<ImmutableList<Actor>>

    /**
     * All [Visible] actors that are currently within the viewport's boundaries.
     */
    abstract val visibleActorsWithinViewport: StateFlow<ImmutableList<Visible>>

    /**
     * All [Dynamic] actors that are currently active and should receive updates.
     */
    abstract val activeDynamicActors: StateFlow<ImmutableList<Dynamic>>

    /**
     * Adds one or more [Actor]s to the game.
     * The actual addition logic will happen in a background thread so the result might not be instantaneous.
     * Multiple calls invoked in the same frame will be batched.
     * Each actor's onAdded() callback function will get invoked just before the actual addition, on the main thread.
     */
    abstract fun add(vararg actors: Actor)

    /**
     * Adds a collection of [Actor]s to the game.
     * The actual addition logic will happen in a background thread so the result might not be instantaneous.
     * Multiple calls invoked in the same frame will be batched.
     * Each actor's onAdded() callback function will get invoked just before the actual addition, on the main thread.
     */
    abstract fun add(actors: Collection<Actor>)

    /**
     * Removes one or more [Actor]s from the game.
     * The actual removal logic will happen in a background thread so the result might not be instantaneous.
     * Multiple calls invoked in the same frame will be batched.
     * Each actor's onRemoved() callback function will get invoked just after the actual removal, on the main thread.
     */
    abstract fun remove(vararg actors: Actor)

    /**
     * Removes a collection of [Actor]s from the game.
     * The actual removal logic will happen in a background thread so the result might not be instantaneous.
     * Multiple calls invoked in the same frame will be batched.
     * Each actor's onRemoved() callback function will get invoked just after the actual removal, on the main thread.
     */
    abstract fun remove(actors: Collection<Actor>)

    /**
     * Removes all [Actor]s from the game.
     * The actual removal logic will happen in a background thread so the result might not be instantaneous.
     * Each actor's onRemoved() callback function will get invoked just after the actual removal, on the main thread.
     */
    abstract fun removeAll()

    companion object {
        /**
         * Creates a new [ActorManager] instance.
         *
         * @param initialActors The actors to start the game with. Their addition might not happen in the very first frame.
         * @param shouldUpdateActorsWhileNotRunning Whether [Dynamic] actors should receive updates even when the game is paused.
         * @param shouldPutFarAwayActorsToSleep Whether [Dynamic] actors far outside the viewport should stop receiving updates.
         * @param invisibleActorMinimumRefreshTimeInMillis Controls the frequency of the automatic visibility check for [Visible] actors.
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name for logging purposes.
         */
        fun newInstance(
            initialActors: List<Actor> = emptyList(),
            shouldUpdateActorsWhileNotRunning: Boolean = false,
            shouldPutFarAwayActorsToSleep: Boolean = true,
            invisibleActorMinimumRefreshTimeInMillis: Long = 0,
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): ActorManager = ActorManagerImpl(
            initialActors = initialActors,
            shouldUpdateActorsWhileNotRunning = shouldUpdateActorsWhileNotRunning,
            shouldPutFarAwayActorsToSleep = shouldPutFarAwayActorsToSleep,
            invisibleActorMinimumRefreshTimeInMillis = invisibleActorMinimumRefreshTimeInMillis,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}
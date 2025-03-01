/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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
 * TODO: Documentation
 */
sealed class ActorManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "ActorManager",
) {
    abstract val allActors: StateFlow<ImmutableList<Actor>>
    abstract val visibleActorsWithinViewport: StateFlow<ImmutableList<Visible>>
    abstract val activeDynamicActors: StateFlow<ImmutableList<Dynamic>>

    abstract fun add(vararg actors: Actor)

    abstract fun add(actors: Collection<Actor>)

    abstract fun remove(vararg actors: Actor)

    abstract fun remove(actors: Collection<Actor>)

    abstract fun removeAll()

    companion object {
        fun newInstance(
            initialActors: List<Actor> = emptyList(),
            shouldUpdateActorsWhileNotRunning: Boolean = false,
            shouldPutFarAwayActorsToSleep: Boolean = true,
            invisibleActorMinimumRefreshTimeInMillis: Long = 100,
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
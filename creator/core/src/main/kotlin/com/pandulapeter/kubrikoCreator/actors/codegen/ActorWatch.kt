/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition

/**
 * A Manager type reacting to Actors of [actorType] (or of a type inheriting from it) being added or removed: its
 * actorAdded or actorRemoved Events. The Actors tell the Manager through [notifyFunction] from `onAdded` and `onRemoved`,
 * which Kubriko calls on a thread of its own, so the Manager only counts them there and runs the Events on its next frame.
 * Both classes are generated from the same list ([of]), so a Manager counts exactly what its Actors tell it.
 */
internal data class ActorWatch(
    val manager: String,
    val actorType: String,
    val isAdded: Boolean,
) {
    val notifyFunction get() = "notify$actorType${if (isAdded) "Added" else "Removed"}"

    companion object {

        /**
         * What [manager] reacts to: its actorAdded Events first, then its actorRemoved ones, each by Actor type, only for
         * the Events whose code is generated and that have Blocks to run.
         */
        fun of(manager: ManagerTypeDefinition, actorTypeNames: Collection<String>) = manager.events
            .filter { (it.type == EventType.ACTOR_ADDED || it.type == EventType.ACTOR_REMOVED) && it.blocks.isNotEmpty() && GeneratedCode.isGenerated(it, BlockTypeKind.MANAGER, actorTypeNames) }
            .map { ActorWatch(manager.name, checkNotNull(it.trigger.otherActorType), it.type == EventType.ACTOR_ADDED) }
            .distinct()
            .sortedWith(compareBy({ !it.isAdded }, { it.actorType }))
    }
}

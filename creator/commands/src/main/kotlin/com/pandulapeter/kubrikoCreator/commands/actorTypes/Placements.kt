/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.commands.PlacementProblem

/**
 * Why a type of [kind] can't have an Event of this type, or null if it can: one reason per Event, named as the editor
 * names it.
 */
internal fun EventType.problemIn(kind: BlockTypeKind): PlacementProblem? = when {
    isAvailableIn(kind) -> null
    kind == BlockTypeKind.ACTOR -> if (this == EventType.GAME_STARTED) PlacementProblem.EVENT_GAME_STARTED_IN_ACTOR_TYPE else PlacementProblem.EVENT_ONLY_IN_MANAGER_TYPE
    else -> when (this) {
        EventType.ADDED_TO_SCENE -> PlacementProblem.EVENT_ADDED_TO_SCENE_IN_MANAGER_TYPE
        EventType.EDGE_TOUCHED -> PlacementProblem.EVENT_EDGE_TOUCHED_IN_MANAGER_TYPE
        EventType.COLLISION -> PlacementProblem.EVENT_COLLISION_IN_MANAGER_TYPE
        else -> PlacementProblem.EVENT_ONLY_IN_ACTOR_TYPE
    }
}

/**
 * Why a Block of this kind can't be in an Event of [eventType] of a type of [kind], or null if it can: what a Manager
 * type lacks comes first, since no Event of one could take the Block, then the Event the Block needs.
 */
internal fun BlockKind.problemIn(eventType: EventType, kind: BlockTypeKind): PlacementProblem? = when {
    isAvailableIn(eventType, kind) -> null
    needsActor && kind == BlockTypeKind.MANAGER -> when (this) {
        BlockKind.REMOVE_SELF -> PlacementProblem.BLOCK_REMOVE_SELF_IN_MANAGER_TYPE
        BlockKind.REMOVE_OTHER -> PlacementProblem.BLOCK_REMOVE_OTHER_IN_MANAGER_TYPE
        BlockKind.RUN_PARENT_EVENT -> PlacementProblem.BLOCK_RUN_PARENT_EVENT_IN_MANAGER_TYPE
        BlockKind.SET_VELOCITY, BlockKind.ADD_TO_VELOCITY, BlockKind.STOP, BlockKind.JUMP_TO, BlockKind.MOVE_TOWARDS_NEAREST, BlockKind.BOUNCE_OFF_EDGE, BlockKind.BOUNCE_OFF_OTHER ->
            PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE
        else -> PlacementProblem.BLOCK_NEEDS_ACTOR_IN_MANAGER_TYPE
    }
    else -> when (onlyIn) {
        EventType.EDGE_TOUCHED -> PlacementProblem.BLOCK_ONLY_IN_EDGE_TOUCHED
        EventType.COLLISION -> PlacementProblem.BLOCK_ONLY_IN_COLLISION
        else -> PlacementProblem.BLOCK_ONLY_IN_ANOTHER_EVENT
    }
}

/**
 * Why a Block can't name a Property of this kind that its type (or the Manager type it names) doesn't have.
 */
internal val PropertyKind.missingProblem: PlacementProblem
    get() = when (this) {
        PropertyKind.NUMBER -> PlacementProblem.NUMBER_PROPERTY_MISSING
        PropertyKind.TEXT -> PlacementProblem.TEXT_PROPERTY_MISSING
        PropertyKind.TOGGLE -> PlacementProblem.TOGGLE_PROPERTY_MISSING
    }

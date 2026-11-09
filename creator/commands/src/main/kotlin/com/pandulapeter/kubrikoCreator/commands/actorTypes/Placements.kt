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
 * names it. Every Event names its own (a new one has to), and `PlacementsTest` holds them to [EventType.isAvailableIn].
 */
internal fun EventType.problemIn(kind: BlockTypeKind): PlacementProblem? = if (isAvailableIn(kind)) {
    null
} else {
    checkNotNull(
        when (this) {
            EventType.GAME_STARTED -> PlacementProblem.EVENT_GAME_STARTED_IN_ACTOR_TYPE
            EventType.ADDED_TO_SCENE -> PlacementProblem.EVENT_ADDED_TO_SCENE_IN_MANAGER_TYPE
            EventType.EDGE_TOUCHED -> PlacementProblem.EVENT_EDGE_TOUCHED_IN_MANAGER_TYPE
            EventType.COLLISION -> PlacementProblem.EVENT_COLLISION_IN_MANAGER_TYPE
            EventType.EVERY_FRAME, EventType.KEY_PRESSED, EventType.TIMER_FINISHED -> null
        },
    ) { "$this has no reason of its own for not being in a $kind type." }
}

/**
 * Why a Block of this kind can't be in an Event of [eventType] of a type of [kind], or null if it can: what a Manager
 * type lacks comes first, since no Event of one could take the Block, then the Event the Block needs.
 */
internal fun BlockKind.problemIn(eventType: EventType, kind: BlockTypeKind): PlacementProblem? = when {
    isAvailableIn(eventType, kind) -> null
    needsActor && kind == BlockTypeKind.MANAGER -> checkNotNull(managerTypeProblem) { "$this has no reason of its own for not being in a Manager type." }
    else -> checkNotNull(otherEventProblem) { "$this has no reason of its own for not being in a $eventType Event." }
}

/**
 * Why a Manager type can't have a Block of this kind, for exactly the kinds that [BlockKind.needsActor]; every Block names
 * its own (a new one has to), and `PlacementsTest` holds them to it.
 */
internal val BlockKind.managerTypeProblem: PlacementProblem?
    get() = when (this) {
        BlockKind.SET_VELOCITY, BlockKind.ADD_TO_VELOCITY, BlockKind.STOP, BlockKind.JUMP_TO, BlockKind.MOVE_TOWARDS_NEAREST, BlockKind.BOUNCE_OFF_EDGE, BlockKind.BOUNCE_OFF_OTHER ->
            PlacementProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE
        BlockKind.REMOVE_SELF -> PlacementProblem.BLOCK_REMOVE_SELF_IN_MANAGER_TYPE
        BlockKind.REMOVE_OTHER -> PlacementProblem.BLOCK_REMOVE_OTHER_IN_MANAGER_TYPE
        BlockKind.RUN_PARENT_EVENT -> PlacementProblem.BLOCK_RUN_PARENT_EVENT_IN_MANAGER_TYPE
        BlockKind.SET_NUMBER, BlockKind.CHANGE_NUMBER, BlockKind.SET_TEXT, BlockKind.SET_TOGGLE, BlockKind.ADD_ACTOR, BlockKind.START_TIMER, BlockKind.STOP_TIMER,
        BlockKind.GO_TO_SCENE, BlockKind.RESTART_SCENE, BlockKind.PAUSE_GAME, BlockKind.PLAY_SOUND, BlockKind.PLAY_MUSIC, BlockKind.STOP_MUSIC, BlockKind.IF,
        BlockKind.ELSE, BlockKind.REPEAT, BlockKind.END, BlockKind.LOG, BlockKind.KOTLIN,
        -> null
    }

/**
 * Why a Block of this kind can't be in an Event other than its [BlockKind.onlyIn], for exactly the kinds that have one;
 * every Block names its own (a new one has to), and `PlacementsTest` holds them to it.
 */
internal val BlockKind.otherEventProblem: PlacementProblem?
    get() = when (this) {
        BlockKind.BOUNCE_OFF_EDGE -> PlacementProblem.BLOCK_BOUNCE_OFF_EDGE_OUTSIDE_EDGE_TOUCHED
        BlockKind.BOUNCE_OFF_OTHER -> PlacementProblem.BLOCK_BOUNCE_OFF_OTHER_OUTSIDE_COLLISION
        BlockKind.REMOVE_OTHER -> PlacementProblem.BLOCK_REMOVE_OTHER_OUTSIDE_COLLISION
        BlockKind.SET_VELOCITY, BlockKind.ADD_TO_VELOCITY, BlockKind.STOP, BlockKind.RUN_PARENT_EVENT, BlockKind.JUMP_TO, BlockKind.REMOVE_SELF, BlockKind.SET_NUMBER,
        BlockKind.CHANGE_NUMBER, BlockKind.SET_TEXT, BlockKind.SET_TOGGLE, BlockKind.ADD_ACTOR, BlockKind.MOVE_TOWARDS_NEAREST, BlockKind.START_TIMER, BlockKind.STOP_TIMER,
        BlockKind.GO_TO_SCENE, BlockKind.RESTART_SCENE, BlockKind.PAUSE_GAME, BlockKind.PLAY_SOUND, BlockKind.PLAY_MUSIC, BlockKind.STOP_MUSIC, BlockKind.IF,
        BlockKind.ELSE, BlockKind.REPEAT, BlockKind.END, BlockKind.LOG, BlockKind.KOTLIN,
        -> null
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

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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem

/**
 * Why a type of [kind] can't have an Event of this type, or null if it can: one reason per Event, named as the editor
 * names it.
 */
internal fun EventType.problemIn(kind: BlockTypeKind): CommandProblem? = if (isAvailableIn(kind)) {
    null
} else {
    when (this) {
        EventType.GAME_STARTED -> CommandProblem.EVENT_GAME_STARTED_IN_ACTOR_TYPE
        EventType.ADDED_TO_SCENE -> CommandProblem.EVENT_ADDED_TO_SCENE_IN_MANAGER_TYPE
        EventType.EDGE_TOUCHED -> CommandProblem.EVENT_EDGE_TOUCHED_IN_MANAGER_TYPE
        EventType.COLLISION -> CommandProblem.EVENT_COLLISION_IN_MANAGER_TYPE
        EventType.EVERY_FRAME, EventType.KEY_PRESSED, EventType.TIMER_FINISHED -> error("$this is available in every type.")
    }
}

/**
 * Why a Block of this kind can't be in an Event of [eventType] of a type of [kind], or null if it can: the Event it
 * needs, or what a Manager type lacks.
 */
internal fun BlockKind.problemIn(eventType: EventType, kind: BlockTypeKind): CommandProblem? = if (isAvailableIn(eventType, kind)) {
    null
} else {
    when (this) {
        BlockKind.BOUNCE_OFF_EDGE -> CommandProblem.BLOCK_BOUNCE_OFF_EDGE_OUTSIDE_EDGE_TOUCHED
        BlockKind.BOUNCE_OFF_OTHER -> CommandProblem.BLOCK_BOUNCE_OFF_OTHER_OUTSIDE_COLLISION
        BlockKind.REMOVE_OTHER -> CommandProblem.BLOCK_REMOVE_OTHER_OUTSIDE_COLLISION
        BlockKind.REMOVE_SELF -> CommandProblem.BLOCK_REMOVE_SELF_IN_MANAGER_TYPE
        BlockKind.RUN_PARENT_EVENT -> CommandProblem.BLOCK_RUN_PARENT_EVENT_IN_MANAGER_TYPE
        BlockKind.SET_VELOCITY, BlockKind.ADD_TO_VELOCITY, BlockKind.STOP, BlockKind.JUMP_TO, BlockKind.MOVE_TOWARDS_NEAREST -> CommandProblem.BLOCK_MOVEMENT_IN_MANAGER_TYPE
        BlockKind.SET_NUMBER,
        BlockKind.CHANGE_NUMBER,
        BlockKind.SET_TEXT,
        BlockKind.SET_TOGGLE,
        BlockKind.ADD_ACTOR,
        BlockKind.START_TIMER,
        BlockKind.STOP_TIMER,
        BlockKind.GO_TO_SCENE,
        BlockKind.RESTART_SCENE,
        BlockKind.PAUSE_GAME,
        BlockKind.PLAY_SOUND,
        BlockKind.PLAY_MUSIC,
        BlockKind.STOP_MUSIC,
        BlockKind.IF,
        BlockKind.ELSE,
        BlockKind.REPEAT,
        BlockKind.END,
        BlockKind.LOG,
        BlockKind.KOTLIN,
        -> error("$this is available in every Event of every type.")
    }
}

/**
 * Why a Block can't name a Property of this kind that its type (or the Manager type it names) doesn't have.
 */
internal val PropertyKind.missingProblem: CommandProblem
    get() = when (this) {
        PropertyKind.NUMBER -> CommandProblem.NUMBER_PROPERTY_MISSING
        PropertyKind.TEXT -> CommandProblem.TEXT_PROPERTY_MISSING
        PropertyKind.TOGGLE -> CommandProblem.TOGGLE_PROPERTY_MISSING
    }

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_category_actors
import kubriko.creator.generated.resources.block_category_code
import kubriko.creator.generated.resources.block_category_flow
import kubriko.creator.generated.resources.block_category_movement
import kubriko.creator.generated.resources.block_category_properties
import kubriko.creator.generated.resources.block_category_timers
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_condition
import kubriko.creator.generated.resources.ic_property
import kubriko.creator.generated.resources.ic_timer
import kubriko.creator.generated.resources.ic_velocity
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/**
 * The tabs of the Block palette, as GameMaker groups its actions, so that only a few Blocks show at a time.
 */
internal enum class BlockCategory(
    val title: StringResource,
    val icon: DrawableResource,
    val kinds: List<BlockKind>,
) {
    MOVEMENT(
        Res.string.block_category_movement,
        Res.drawable.ic_velocity,
        listOf(
            BlockKind.SET_VELOCITY,
            BlockKind.ADD_TO_VELOCITY,
            BlockKind.STOP,
            BlockKind.JUMP_TO,
            BlockKind.MOVE_TOWARDS_NEAREST,
            BlockKind.BOUNCE_OFF_EDGE,
            BlockKind.BOUNCE_OFF_OTHER,
        ),
    ),
    ACTORS(Res.string.block_category_actors, Res.drawable.ic_actor_type, listOf(BlockKind.ADD_ACTOR, BlockKind.REMOVE_SELF, BlockKind.REMOVE_OTHER)),
    PROPERTIES(Res.string.block_category_properties, Res.drawable.ic_property, listOf(BlockKind.SET_NUMBER, BlockKind.CHANGE_NUMBER, BlockKind.SET_TEXT, BlockKind.SET_TOGGLE)),
    FLOW(Res.string.block_category_flow, Res.drawable.ic_condition, listOf(BlockKind.IF, BlockKind.ELSE, BlockKind.REPEAT, BlockKind.END)),
    TIMERS(Res.string.block_category_timers, Res.drawable.ic_timer, listOf(BlockKind.START_TIMER, BlockKind.STOP_TIMER)),
    CODE(Res.string.block_category_code, Res.drawable.ic_code, listOf(BlockKind.LOG, BlockKind.KOTLIN));

    companion object {

        /**
         * The categories that have a Block for an Event of [eventType] of a type of [typeKind], which are the tabs shown.
         * Code is always one of them.
         */
        fun availableIn(eventType: EventType, typeKind: BlockTypeKind) = entries.filter { category -> category.kinds.any { it.isAvailableIn(eventType, typeKind) } }
    }
}

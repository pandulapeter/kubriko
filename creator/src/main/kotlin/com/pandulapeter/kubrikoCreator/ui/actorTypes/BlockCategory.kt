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
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_category_actors
import kubriko.creator.generated.resources.block_category_audio
import kubriko.creator.generated.resources.block_category_code
import kubriko.creator.generated.resources.block_category_flow
import kubriko.creator.generated.resources.block_category_movement
import kubriko.creator.generated.resources.block_category_properties
import kubriko.creator.generated.resources.block_category_scenes
import kubriko.creator.generated.resources.block_category_timers
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_condition
import kubriko.creator.generated.resources.ic_property
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.ic_sound
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
    val accent: Accent,
    val kinds: List<BlockKind>,
) {
    MOVEMENT(
        Res.string.block_category_movement,
        Res.drawable.ic_velocity,
        Accent.MOVEMENT,
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
    ACTORS(Res.string.block_category_actors, Res.drawable.ic_actor_type, Accent.CAST, listOf(BlockKind.ADD_ACTOR, BlockKind.REMOVE_SELF, BlockKind.REMOVE_OTHER)),
    PROPERTIES(Res.string.block_category_properties, Res.drawable.ic_property, Accent.PROPERTIES, listOf(BlockKind.SET_NUMBER, BlockKind.CHANGE_NUMBER, BlockKind.SET_TEXT, BlockKind.SET_TOGGLE)),
    FLOW(Res.string.block_category_flow, Res.drawable.ic_condition, Accent.FLOW, listOf(BlockKind.IF, BlockKind.ELSE, BlockKind.REPEAT, BlockKind.END, BlockKind.RUN_PARENT_EVENT)),
    TIMERS(Res.string.block_category_timers, Res.drawable.ic_timer, Accent.TIMERS, listOf(BlockKind.START_TIMER, BlockKind.STOP_TIMER)),
    SCENES(Res.string.block_category_scenes, Res.drawable.ic_scene, Accent.SCENE, listOf(BlockKind.GO_TO_SCENE, BlockKind.RESTART_SCENE, BlockKind.PAUSE_GAME)),
    AUDIO(Res.string.block_category_audio, Res.drawable.ic_sound, Accent.AUDIO, listOf(BlockKind.PLAY_SOUND, BlockKind.PLAY_MUSIC, BlockKind.STOP_MUSIC)),
    CODE(Res.string.block_category_code, Res.drawable.ic_code, Accent.CODE, listOf(BlockKind.LOG, BlockKind.KOTLIN));

    companion object {

        /**
         * The categories that have a Block for an Event of [eventType] of a type of [typeKind], which are the tabs shown.
         * Code is always one of them.
         */
        fun availableIn(eventType: EventType, typeKind: BlockTypeKind) = entries.filter { category -> category.kinds.any { it.isAvailableIn(eventType, typeKind) } }
    }
}

/**
 * The palette category this Block is in, whose [BlockCategory.accent] it is drawn with.
 */
internal val BlockKind.category get() = BlockCategory.entries.first { this in it.kinds }

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
import com.pandulapeter.kubrikoCreator.actors.ConditionKind
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_add_actor
import kubriko.creator.generated.resources.block_add_to_velocity
import kubriko.creator.generated.resources.block_bounce_off_edge
import kubriko.creator.generated.resources.block_bounce_off_other
import kubriko.creator.generated.resources.block_change_number
import kubriko.creator.generated.resources.block_else
import kubriko.creator.generated.resources.block_end
import kubriko.creator.generated.resources.block_go_to_scene
import kubriko.creator.generated.resources.block_if
import kubriko.creator.generated.resources.block_jump_to
import kubriko.creator.generated.resources.block_kotlin
import kubriko.creator.generated.resources.block_log
import kubriko.creator.generated.resources.block_move_towards_nearest
import kubriko.creator.generated.resources.block_pause_game
import kubriko.creator.generated.resources.block_play_music
import kubriko.creator.generated.resources.block_play_sound
import kubriko.creator.generated.resources.block_remove_other
import kubriko.creator.generated.resources.block_remove_self
import kubriko.creator.generated.resources.block_repeat
import kubriko.creator.generated.resources.block_restart_scene
import kubriko.creator.generated.resources.block_run_parent_event
import kubriko.creator.generated.resources.block_set_number
import kubriko.creator.generated.resources.block_set_text
import kubriko.creator.generated.resources.block_set_toggle
import kubriko.creator.generated.resources.block_set_velocity
import kubriko.creator.generated.resources.block_start_timer
import kubriko.creator.generated.resources.block_stop
import kubriko.creator.generated.resources.block_stop_music
import kubriko.creator.generated.resources.block_stop_timer
import kubriko.creator.generated.resources.condition_chance
import kubriko.creator.generated.resources.condition_key_held
import kubriko.creator.generated.resources.condition_number
import kubriko.creator.generated.resources.condition_paused
import kubriko.creator.generated.resources.condition_text
import kubriko.creator.generated.resources.condition_toggle
import kubriko.creator.generated.resources.condition_velocity
import kubriko.creator.generated.resources.property_kind_number
import kubriko.creator.generated.resources.property_kind_text
import kubriko.creator.generated.resources.property_kind_toggle
import org.jetbrains.compose.resources.StringResource

internal val BlockKind.title: StringResource
    get() = when (this) {
        BlockKind.SET_VELOCITY -> Res.string.block_set_velocity
        BlockKind.ADD_TO_VELOCITY -> Res.string.block_add_to_velocity
        BlockKind.STOP -> Res.string.block_stop
        BlockKind.RUN_PARENT_EVENT -> Res.string.block_run_parent_event
        BlockKind.JUMP_TO -> Res.string.block_jump_to
        BlockKind.BOUNCE_OFF_EDGE -> Res.string.block_bounce_off_edge
        BlockKind.REMOVE_SELF -> Res.string.block_remove_self
        BlockKind.BOUNCE_OFF_OTHER -> Res.string.block_bounce_off_other
        BlockKind.REMOVE_OTHER -> Res.string.block_remove_other
        BlockKind.SET_NUMBER -> Res.string.block_set_number
        BlockKind.CHANGE_NUMBER -> Res.string.block_change_number
        BlockKind.SET_TEXT -> Res.string.block_set_text
        BlockKind.SET_TOGGLE -> Res.string.block_set_toggle
        BlockKind.ADD_ACTOR -> Res.string.block_add_actor
        BlockKind.MOVE_TOWARDS_NEAREST -> Res.string.block_move_towards_nearest
        BlockKind.START_TIMER -> Res.string.block_start_timer
        BlockKind.STOP_TIMER -> Res.string.block_stop_timer
        BlockKind.GO_TO_SCENE -> Res.string.block_go_to_scene
        BlockKind.RESTART_SCENE -> Res.string.block_restart_scene
        BlockKind.PAUSE_GAME -> Res.string.block_pause_game
        BlockKind.PLAY_SOUND -> Res.string.block_play_sound
        BlockKind.PLAY_MUSIC -> Res.string.block_play_music
        BlockKind.STOP_MUSIC -> Res.string.block_stop_music
        BlockKind.IF -> Res.string.block_if
        BlockKind.ELSE -> Res.string.block_else
        BlockKind.REPEAT -> Res.string.block_repeat
        BlockKind.END -> Res.string.block_end
        BlockKind.LOG -> Res.string.block_log
        BlockKind.KOTLIN -> Res.string.block_kotlin
    }

internal val ConditionKind.title: StringResource
    get() = when (this) {
        ConditionKind.NUMBER -> Res.string.condition_number
        ConditionKind.TEXT -> Res.string.condition_text
        ConditionKind.TOGGLE -> Res.string.condition_toggle
        ConditionKind.KEY_HELD -> Res.string.condition_key_held
        ConditionKind.VELOCITY -> Res.string.condition_velocity
        ConditionKind.CHANCE -> Res.string.condition_chance
        ConditionKind.PAUSED -> Res.string.condition_paused
    }

internal val PropertyKind.title: StringResource
    get() = when (this) {
        PropertyKind.NUMBER -> Res.string.property_kind_number
        PropertyKind.TEXT -> Res.string.property_kind_text
        PropertyKind.TOGGLE -> Res.string.property_kind_toggle
    }

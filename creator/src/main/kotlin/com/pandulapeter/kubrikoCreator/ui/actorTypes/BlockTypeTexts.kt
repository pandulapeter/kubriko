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
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.PauseAction
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.ScreenPosition
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_pause_action_pause
import kubriko.creator.generated.resources.block_pause_action_resume
import kubriko.creator.generated.resources.block_pause_action_toggle
import kubriko.creator.generated.resources.event_added_to_scene
import kubriko.creator.generated.resources.event_collision
import kubriko.creator.generated.resources.event_edge_touched
import kubriko.creator.generated.resources.event_every_frame
import kubriko.creator.generated.resources.event_game_started
import kubriko.creator.generated.resources.event_key_pressed
import kubriko.creator.generated.resources.event_timer_finished
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_add_actor
import kubriko.creator.generated.resources.ic_bounce
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_collision
import kubriko.creator.generated.resources.ic_condition
import kubriko.creator.generated.resources.ic_console
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_edge
import kubriko.creator.generated.resources.ic_end
import kubriko.creator.generated.resources.ic_keyboard
import kubriko.creator.generated.resources.ic_loop
import kubriko.creator.generated.resources.ic_manager_type
import kubriko.creator.generated.resources.ic_move_towards
import kubriko.creator.generated.resources.ic_music
import kubriko.creator.generated.resources.ic_music_off
import kubriko.creator.generated.resources.ic_number
import kubriko.creator.generated.resources.ic_open_with
import kubriko.creator.generated.resources.ic_pause
import kubriko.creator.generated.resources.ic_play
import kubriko.creator.generated.resources.ic_refresh
import kubriko.creator.generated.resources.ic_remove_other
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.ic_sound
import kubriko.creator.generated.resources.ic_stop
import kubriko.creator.generated.resources.ic_text
import kubriko.creator.generated.resources.ic_timer
import kubriko.creator.generated.resources.ic_timer_off
import kubriko.creator.generated.resources.ic_toggle
import kubriko.creator.generated.resources.ic_trending_up
import kubriko.creator.generated.resources.ic_velocity
import kubriko.creator.generated.resources.key_a
import kubriko.creator.generated.resources.key_d
import kubriko.creator.generated.resources.key_down
import kubriko.creator.generated.resources.key_enter
import kubriko.creator.generated.resources.key_left
import kubriko.creator.generated.resources.key_right
import kubriko.creator.generated.resources.key_s
import kubriko.creator.generated.resources.key_space
import kubriko.creator.generated.resources.key_up
import kubriko.creator.generated.resources.key_w
import kubriko.creator.generated.resources.screen_position_bottom
import kubriko.creator.generated.resources.screen_position_bottom_left
import kubriko.creator.generated.resources.screen_position_bottom_right
import kubriko.creator.generated.resources.screen_position_center
import kubriko.creator.generated.resources.screen_position_left
import kubriko.creator.generated.resources.screen_position_right
import kubriko.creator.generated.resources.screen_position_top
import kubriko.creator.generated.resources.screen_position_top_left
import kubriko.creator.generated.resources.screen_position_top_right
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

internal val EventType.title: StringResource
    get() = when (this) {
        EventType.GAME_STARTED -> Res.string.event_game_started
        EventType.ADDED_TO_SCENE -> Res.string.event_added_to_scene
        EventType.EVERY_FRAME -> Res.string.event_every_frame
        EventType.KEY_PRESSED -> Res.string.event_key_pressed
        EventType.EDGE_TOUCHED -> Res.string.event_edge_touched
        EventType.COLLISION -> Res.string.event_collision
        EventType.TIMER_FINISHED -> Res.string.event_timer_finished
    }

internal val EventType.icon: DrawableResource
    get() = when (this) {
        EventType.GAME_STARTED -> Res.drawable.ic_play
        EventType.ADDED_TO_SCENE -> Res.drawable.ic_add
        EventType.EVERY_FRAME -> Res.drawable.ic_loop
        EventType.KEY_PRESSED -> Res.drawable.ic_keyboard
        EventType.EDGE_TOUCHED -> Res.drawable.ic_edge
        EventType.COLLISION -> Res.drawable.ic_collision
        EventType.TIMER_FINISHED -> Res.drawable.ic_timer
    }

internal val BlockKind.icon: DrawableResource
    get() = when (this) {
        BlockKind.SET_VELOCITY -> Res.drawable.ic_velocity
        BlockKind.ADD_TO_VELOCITY -> Res.drawable.ic_trending_up
        BlockKind.STOP -> Res.drawable.ic_stop
        BlockKind.JUMP_TO -> Res.drawable.ic_open_with
        BlockKind.BOUNCE_OFF_EDGE -> Res.drawable.ic_bounce
        BlockKind.REMOVE_SELF -> Res.drawable.ic_delete
        BlockKind.BOUNCE_OFF_OTHER -> Res.drawable.ic_bounce
        BlockKind.REMOVE_OTHER -> Res.drawable.ic_remove_other
        BlockKind.SET_NUMBER -> Res.drawable.ic_number
        BlockKind.CHANGE_NUMBER -> Res.drawable.ic_add
        BlockKind.SET_TEXT -> Res.drawable.ic_text
        BlockKind.SET_TOGGLE -> Res.drawable.ic_toggle
        BlockKind.ADD_ACTOR -> Res.drawable.ic_add_actor
        BlockKind.MOVE_TOWARDS_NEAREST -> Res.drawable.ic_move_towards
        BlockKind.START_TIMER -> Res.drawable.ic_timer
        BlockKind.STOP_TIMER -> Res.drawable.ic_timer_off
        BlockKind.GO_TO_SCENE -> Res.drawable.ic_scene
        BlockKind.RESTART_SCENE -> Res.drawable.ic_refresh
        BlockKind.PAUSE_GAME -> Res.drawable.ic_pause
        BlockKind.PLAY_SOUND -> Res.drawable.ic_sound
        BlockKind.PLAY_MUSIC -> Res.drawable.ic_music
        BlockKind.STOP_MUSIC -> Res.drawable.ic_music_off
        BlockKind.IF, BlockKind.ELSE -> Res.drawable.ic_condition
        BlockKind.REPEAT -> Res.drawable.ic_loop
        BlockKind.END -> Res.drawable.ic_end
        BlockKind.RUN_PARENT_EVENT -> Res.drawable.ic_actor_type
        BlockKind.LOG -> Res.drawable.ic_console
        BlockKind.KOTLIN -> Res.drawable.ic_code
    }

internal val PauseAction.title: StringResource
    get() = when (this) {
        PauseAction.PAUSE -> Res.string.block_pause_action_pause
        PauseAction.RESUME -> Res.string.block_pause_action_resume
        PauseAction.TOGGLE -> Res.string.block_pause_action_toggle
    }

internal val KeyChoice.title: StringResource
    get() = when (this) {
        KeyChoice.LEFT -> Res.string.key_left
        KeyChoice.RIGHT -> Res.string.key_right
        KeyChoice.UP -> Res.string.key_up
        KeyChoice.DOWN -> Res.string.key_down
        KeyChoice.SPACE -> Res.string.key_space
        KeyChoice.ENTER -> Res.string.key_enter
        KeyChoice.W -> Res.string.key_w
        KeyChoice.A -> Res.string.key_a
        KeyChoice.S -> Res.string.key_s
        KeyChoice.D -> Res.string.key_d
    }

internal val PropertyKind.icon: DrawableResource
    get() = when (this) {
        PropertyKind.NUMBER -> Res.drawable.ic_number
        PropertyKind.TEXT -> Res.drawable.ic_text
        PropertyKind.TOGGLE -> Res.drawable.ic_toggle
    }

internal val ScreenPosition.title: StringResource
    get() = when (this) {
        ScreenPosition.TOP_LEFT -> Res.string.screen_position_top_left
        ScreenPosition.TOP -> Res.string.screen_position_top
        ScreenPosition.TOP_RIGHT -> Res.string.screen_position_top_right
        ScreenPosition.LEFT -> Res.string.screen_position_left
        ScreenPosition.CENTER -> Res.string.screen_position_center
        ScreenPosition.RIGHT -> Res.string.screen_position_right
        ScreenPosition.BOTTOM_LEFT -> Res.string.screen_position_bottom_left
        ScreenPosition.BOTTOM -> Res.string.screen_position_bottom
        ScreenPosition.BOTTOM_RIGHT -> Res.string.screen_position_bottom_right
    }

internal val BlockTypeKind.icon: DrawableResource
    get() = when (this) {
        BlockTypeKind.ACTOR -> Res.drawable.ic_actor_type
        BlockTypeKind.MANAGER -> Res.drawable.ic_manager_type
    }

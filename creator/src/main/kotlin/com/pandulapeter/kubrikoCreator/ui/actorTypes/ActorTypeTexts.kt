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
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.event_added_to_scene
import kubriko.creator.generated.resources.event_collision
import kubriko.creator.generated.resources.event_edge_touched
import kubriko.creator.generated.resources.event_every_frame
import kubriko.creator.generated.resources.event_key_pressed
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_bounce
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_collision
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_edge
import kubriko.creator.generated.resources.ic_keyboard
import kubriko.creator.generated.resources.ic_loop
import kubriko.creator.generated.resources.ic_open_with
import kubriko.creator.generated.resources.ic_remove_other
import kubriko.creator.generated.resources.ic_stop
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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

internal val EventType.title: StringResource
    get() = when (this) {
        EventType.ADDED_TO_SCENE -> Res.string.event_added_to_scene
        EventType.EVERY_FRAME -> Res.string.event_every_frame
        EventType.KEY_PRESSED -> Res.string.event_key_pressed
        EventType.EDGE_TOUCHED -> Res.string.event_edge_touched
        EventType.COLLISION -> Res.string.event_collision
    }

internal val EventType.icon: DrawableResource
    get() = when (this) {
        EventType.ADDED_TO_SCENE -> Res.drawable.ic_add
        EventType.EVERY_FRAME -> Res.drawable.ic_loop
        EventType.KEY_PRESSED -> Res.drawable.ic_keyboard
        EventType.EDGE_TOUCHED -> Res.drawable.ic_edge
        EventType.COLLISION -> Res.drawable.ic_collision
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
        BlockKind.KOTLIN -> Res.drawable.ic_code
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

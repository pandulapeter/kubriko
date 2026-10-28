/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

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
import org.jetbrains.compose.resources.StringResource

/**
 * The palette of Blocks, in the order the editor offers them.
 */
internal enum class BlockKind(
    val title: StringResource,
    val createDefault: () -> BlockDefinition,
    val needsActor: Boolean = false,
) {
    SET_VELOCITY(Res.string.block_set_velocity, { BlockDefinition.SetVelocity(x = 200f, y = 0f) }, needsActor = true),
    ADD_TO_VELOCITY(Res.string.block_add_to_velocity, { BlockDefinition.AddToVelocity(x = 50f, y = 0f) }, needsActor = true),
    STOP(Res.string.block_stop, { BlockDefinition.Stop }, needsActor = true),
    RUN_PARENT_EVENT(Res.string.block_run_parent_event, { BlockDefinition.RunParentEvent }, needsActor = true),
    JUMP_TO(Res.string.block_jump_to, { BlockDefinition.JumpTo() }, needsActor = true),
    BOUNCE_OFF_EDGE(Res.string.block_bounce_off_edge, { BlockDefinition.BounceOffEdge }, needsActor = true),
    REMOVE_SELF(Res.string.block_remove_self, { BlockDefinition.RemoveSelf }, needsActor = true),
    BOUNCE_OFF_OTHER(Res.string.block_bounce_off_other, { BlockDefinition.BounceOffOther }, needsActor = true),
    REMOVE_OTHER(Res.string.block_remove_other, { BlockDefinition.RemoveOther }, needsActor = true),
    SET_NUMBER(Res.string.block_set_number, { BlockDefinition.SetNumber() }),
    CHANGE_NUMBER(Res.string.block_change_number, { BlockDefinition.ChangeNumber() }),
    SET_TEXT(Res.string.block_set_text, { BlockDefinition.SetText() }),
    SET_TOGGLE(Res.string.block_set_toggle, { BlockDefinition.SetToggle() }),
    ADD_ACTOR(Res.string.block_add_actor, { BlockDefinition.AddActor() }),
    MOVE_TOWARDS_NEAREST(Res.string.block_move_towards_nearest, { BlockDefinition.MoveTowardsNearest() }, needsActor = true),
    START_TIMER(Res.string.block_start_timer, { BlockDefinition.StartTimer() }),
    STOP_TIMER(Res.string.block_stop_timer, { BlockDefinition.StopTimer() }),
    GO_TO_SCENE(Res.string.block_go_to_scene, { BlockDefinition.GoToScene() }),
    RESTART_SCENE(Res.string.block_restart_scene, { BlockDefinition.RestartScene }),
    PAUSE_GAME(Res.string.block_pause_game, { BlockDefinition.PauseGame() }),
    PLAY_SOUND(Res.string.block_play_sound, { BlockDefinition.PlaySound() }),
    PLAY_MUSIC(Res.string.block_play_music, { BlockDefinition.PlayMusic() }),
    STOP_MUSIC(Res.string.block_stop_music, { BlockDefinition.StopMusic }),
    IF(Res.string.block_if, { BlockDefinition.If() }),
    ELSE(Res.string.block_else, { BlockDefinition.Else }),
    REPEAT(Res.string.block_repeat, { BlockDefinition.Repeat() }),
    END(Res.string.block_end, { BlockDefinition.End }),
    LOG(Res.string.block_log, { BlockDefinition.Log() }),
    KOTLIN(Res.string.block_kotlin, { BlockDefinition.Kotlin() });

    /**
     * Whether the palette offers this Block in an Event of [eventType] of a type of [kind]. Some Blocks need what only
     * one Event knows: which edge was touched, or which Actor was collided with. Those that move or remove the Actor
     * need one, so a Manager doesn't have them.
     */
    fun isAvailableIn(eventType: EventType, kind: BlockTypeKind) = when {
        needsActor && kind != BlockTypeKind.ACTOR -> false
        this == BOUNCE_OFF_EDGE -> eventType == EventType.EDGE_TOUCHED
        this == BOUNCE_OFF_OTHER || this == REMOVE_OTHER -> eventType == EventType.COLLISION
        else -> true
    }

    companion object {

        fun of(block: BlockDefinition) = when (block) {
            is BlockDefinition.SetVelocity -> SET_VELOCITY
            is BlockDefinition.AddToVelocity -> ADD_TO_VELOCITY
            BlockDefinition.Stop -> STOP
            BlockDefinition.RunParentEvent -> RUN_PARENT_EVENT
            is BlockDefinition.JumpTo -> JUMP_TO
            BlockDefinition.BounceOffEdge -> BOUNCE_OFF_EDGE
            BlockDefinition.RemoveSelf -> REMOVE_SELF
            BlockDefinition.BounceOffOther -> BOUNCE_OFF_OTHER
            BlockDefinition.RemoveOther -> REMOVE_OTHER
            is BlockDefinition.SetNumber -> SET_NUMBER
            is BlockDefinition.ChangeNumber -> CHANGE_NUMBER
            is BlockDefinition.SetText -> SET_TEXT
            is BlockDefinition.SetToggle -> SET_TOGGLE
            is BlockDefinition.AddActor -> ADD_ACTOR
            is BlockDefinition.MoveTowardsNearest -> MOVE_TOWARDS_NEAREST
            is BlockDefinition.StartTimer -> START_TIMER
            is BlockDefinition.StopTimer -> STOP_TIMER
            is BlockDefinition.GoToScene -> GO_TO_SCENE
            BlockDefinition.RestartScene -> RESTART_SCENE
            is BlockDefinition.PauseGame -> PAUSE_GAME
            is BlockDefinition.PlaySound -> PLAY_SOUND
            is BlockDefinition.PlayMusic -> PLAY_MUSIC
            BlockDefinition.StopMusic -> STOP_MUSIC
            is BlockDefinition.If -> IF
            BlockDefinition.Else -> ELSE
            is BlockDefinition.Repeat -> REPEAT
            BlockDefinition.End -> END
            is BlockDefinition.Log -> LOG
            is BlockDefinition.Kotlin -> KOTLIN
        }
    }
}

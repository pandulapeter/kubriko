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
import kubriko.creator.generated.resources.block_add_to_velocity
import kubriko.creator.generated.resources.block_bounce_off_edge
import kubriko.creator.generated.resources.block_bounce_off_other
import kubriko.creator.generated.resources.block_change_number
import kubriko.creator.generated.resources.block_else
import kubriko.creator.generated.resources.block_end
import kubriko.creator.generated.resources.block_if
import kubriko.creator.generated.resources.block_jump_to
import kubriko.creator.generated.resources.block_kotlin
import kubriko.creator.generated.resources.block_log
import kubriko.creator.generated.resources.block_remove_other
import kubriko.creator.generated.resources.block_remove_self
import kubriko.creator.generated.resources.block_repeat
import kubriko.creator.generated.resources.block_set_number
import kubriko.creator.generated.resources.block_set_text
import kubriko.creator.generated.resources.block_set_toggle
import kubriko.creator.generated.resources.block_set_velocity
import kubriko.creator.generated.resources.block_stop
import org.jetbrains.compose.resources.StringResource

/**
 * The palette of Blocks, in the order the editor offers them.
 */
internal enum class BlockKind(
    val title: StringResource,
    val createDefault: () -> BlockDefinition,
) {
    SET_VELOCITY(Res.string.block_set_velocity, { BlockDefinition.SetVelocity(x = 200f, y = 0f) }),
    ADD_TO_VELOCITY(Res.string.block_add_to_velocity, { BlockDefinition.AddToVelocity(x = 50f, y = 0f) }),
    STOP(Res.string.block_stop, { BlockDefinition.Stop }),
    JUMP_TO(Res.string.block_jump_to, { BlockDefinition.JumpTo() }),
    BOUNCE_OFF_EDGE(Res.string.block_bounce_off_edge, { BlockDefinition.BounceOffEdge }),
    REMOVE_SELF(Res.string.block_remove_self, { BlockDefinition.RemoveSelf }),
    BOUNCE_OFF_OTHER(Res.string.block_bounce_off_other, { BlockDefinition.BounceOffOther }),
    REMOVE_OTHER(Res.string.block_remove_other, { BlockDefinition.RemoveOther }),
    SET_NUMBER(Res.string.block_set_number, { BlockDefinition.SetNumber() }),
    CHANGE_NUMBER(Res.string.block_change_number, { BlockDefinition.ChangeNumber() }),
    SET_TEXT(Res.string.block_set_text, { BlockDefinition.SetText() }),
    SET_TOGGLE(Res.string.block_set_toggle, { BlockDefinition.SetToggle() }),
    IF(Res.string.block_if, { BlockDefinition.If() }),
    ELSE(Res.string.block_else, { BlockDefinition.Else }),
    REPEAT(Res.string.block_repeat, { BlockDefinition.Repeat() }),
    END(Res.string.block_end, { BlockDefinition.End }),
    LOG(Res.string.block_log, { BlockDefinition.Log() }),
    KOTLIN(Res.string.block_kotlin, { BlockDefinition.Kotlin() });

    /**
     * Whether the palette offers this Block in an Event of [eventType]. Some Blocks need what only one Event knows:
     * which edge was touched, or which Actor was collided with.
     */
    fun isAvailableIn(eventType: EventType) = when (this) {
        BOUNCE_OFF_EDGE -> eventType == EventType.EDGE_TOUCHED
        BOUNCE_OFF_OTHER, REMOVE_OTHER -> eventType == EventType.COLLISION
        else -> true
    }

    companion object {

        fun of(block: BlockDefinition) = when (block) {
            is BlockDefinition.SetVelocity -> SET_VELOCITY
            is BlockDefinition.AddToVelocity -> ADD_TO_VELOCITY
            BlockDefinition.Stop -> STOP
            is BlockDefinition.JumpTo -> JUMP_TO
            BlockDefinition.BounceOffEdge -> BOUNCE_OFF_EDGE
            BlockDefinition.RemoveSelf -> REMOVE_SELF
            BlockDefinition.BounceOffOther -> BOUNCE_OFF_OTHER
            BlockDefinition.RemoveOther -> REMOVE_OTHER
            is BlockDefinition.SetNumber -> SET_NUMBER
            is BlockDefinition.ChangeNumber -> CHANGE_NUMBER
            is BlockDefinition.SetText -> SET_TEXT
            is BlockDefinition.SetToggle -> SET_TOGGLE
            is BlockDefinition.If -> IF
            BlockDefinition.Else -> ELSE
            is BlockDefinition.Repeat -> REPEAT
            BlockDefinition.End -> END
            is BlockDefinition.Log -> LOG
            is BlockDefinition.Kotlin -> KOTLIN
        }
    }
}

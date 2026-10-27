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
import kubriko.creator.generated.resources.block_jump_to
import kubriko.creator.generated.resources.block_kotlin
import kubriko.creator.generated.resources.block_remove_self
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
    KOTLIN(Res.string.block_kotlin, { BlockDefinition.Kotlin() });

    /**
     * Whether the palette offers this Block in an Event of [eventType]. Bouncing needs to know which edge was touched.
     */
    fun isAvailableIn(eventType: EventType) = this != BOUNCE_OFF_EDGE || eventType == EventType.EDGE_TOUCHED

    companion object {

        fun of(block: BlockDefinition) = when (block) {
            is BlockDefinition.SetVelocity -> SET_VELOCITY
            is BlockDefinition.AddToVelocity -> ADD_TO_VELOCITY
            BlockDefinition.Stop -> STOP
            is BlockDefinition.JumpTo -> JUMP_TO
            BlockDefinition.BounceOffEdge -> BOUNCE_OFF_EDGE
            BlockDefinition.RemoveSelf -> REMOVE_SELF
            is BlockDefinition.Kotlin -> KOTLIN
        }
    }
}

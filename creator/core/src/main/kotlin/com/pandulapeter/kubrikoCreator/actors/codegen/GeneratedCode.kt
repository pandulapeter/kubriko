/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.TimerNaming

/**
 * Where the generated code holds an Event's or a Block's code, so the generators and what decides the plugins a game uses
 * ([PluginUse], [PluginManagers]) agree: a Block left as a comment needs no plugin.
 */
internal object GeneratedCode {

    /**
     * Whether the code of [event] of a type of [kind] is generated: an Event the type can't have, or with what doesn't
     * exist (a key, the other Actor type among [actorTypeNames], or a Timer name), is left out, so the code compiles.
     */
    fun isGenerated(event: EventDefinition, kind: BlockTypeKind, actorTypeNames: Collection<String>) = event.type.isAvailableIn(kind) && when (event.type) {
        EventType.KEY_PRESSED -> event.trigger.key?.isKnown == true
        EventType.COLLISION -> event.trigger.otherActorType in actorTypeNames
        EventType.TIMER_FINISHED -> event.trigger.timer?.let(TimerNaming::isValid) == true
        EventType.ADDED_TO_SCENE, EventType.EVERY_FRAME, EventType.EDGE_TOUCHED, EventType.LEFT_SCENE, EventType.GAME_STARTED -> true
    }

    /**
     * Whether [block] in an Event of [eventType] of a type of [kind] is generated as code rather than as a comment.
     */
    fun isGenerated(block: BlockDefinition, eventType: EventType, kind: BlockTypeKind) = BlockKind.of(block).isAvailableIn(eventType, kind)
}

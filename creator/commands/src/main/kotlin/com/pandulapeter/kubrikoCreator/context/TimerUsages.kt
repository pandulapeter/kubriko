/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.timerName

/**
 * The usages of the Timer [subject] ([ProjectReferences.usagesOf]): the timerFinished Events reacting to it, removed
 * with their Blocks, and the Blocks starting or stopping it outside them, in every type sharing it: the whole family of
 * an Actor type ([timerSharersOf]), or a Manager type alone.
 */
internal fun timerUsagesOf(subject: RelationSubject.Timer, types: List<BlockTypeDefinition>, family: ActorTypeFamily): List<Usage> = buildList {
    val sharers = timerSharersOf(subject.owner, types, family).toSet()
    types.filter { it.name in sharers }.forEach { type ->
        type.events.forEachIndexed { eventIndex, event ->
            if (event.timer == subject.name) {
                add(Usage(UsagePlace.InModel(RelationPlace.Type(type.name, eventIndex)), RelationKind.USES_TIMER, subject, UsageEffect.EVENT_REMOVED))
                return@forEachIndexed
            }
            event.blocks.forEachIndexed { blockIndex, block ->
                if (block.timerName == subject.name) {
                    add(Usage(UsagePlace.InModel(RelationPlace.Type(type.name, eventIndex, blockIndex)), RelationKind.USES_TIMER, subject, UsageEffect.BLOCK_REMOVED))
                }
            }
        }
    }
}

/**
 * The types whose generated classes share the Timers of [typeName]: every Actor type in its family, or the Manager type
 * alone.
 */
internal fun timerSharersOf(typeName: String, types: List<BlockTypeDefinition>, family: ActorTypeFamily): List<String> =
    if (types.firstOrNull { it.name == typeName } is ActorTypeDefinition) family.familyOf(family.rootOf(typeName)) else listOf(typeName)

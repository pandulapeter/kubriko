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
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.KotlinIdentifiers
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.TimerNaming
import com.pandulapeter.kubrikoCreator.actors.timerName
import com.pandulapeter.kubrikoCreator.actors.timerSharersOf

/**
 * The usages of the Timer [subject] ([ProjectReferences.usagesOf]), each with the Timer as [timerOf] names it: the
 * timerFinished Events reacting to it, removed with their Blocks, the Blocks starting or stopping it outside them, and
 * the Kotlin using its generated members ([TimerNaming.generatedNamesOf]), left as it is, in every type sharing it
 * ([timerSharersOf]).
 */
internal fun timerUsagesOf(subject: RelationSubject.Timer, types: List<BlockTypeDefinition>, family: ActorTypeFamily): List<Usage> = buildList {
    val timer = timerOf(subject, types, family)
    val sharers = timerSharersOf(subject.owner, types, family).toSet()
    val generatedNames = TimerNaming.generatedNamesOf(subject.name).toSet()
    fun isUsingMembers(code: String) = KotlinIdentifiers.find(code).any { it.name in generatedNames && (it.receiver == null || it.receiver == "this") }
    types.filter { it.name in sharers }.forEach { type ->
        type.events.forEachIndexed { eventIndex, event ->
            if (event.trigger.timer == subject.name) {
                add(Usage(UsagePlace.InModel(RelationPlace.Type(type.name, eventIndex)), RelationKind.USES_TIMER, timer, UsageEffect.EVENT_REMOVED))
                return@forEachIndexed
            }
            event.blocks.forEachIndexed { blockIndex, block ->
                val place = UsagePlace.InModel(RelationPlace.Type(type.name, eventIndex, blockIndex))
                when {
                    block.timerName == subject.name -> add(Usage(place, RelationKind.USES_TIMER, timer, UsageEffect.BLOCK_REMOVED))
                    block is BlockDefinition.Kotlin && isUsingMembers(block.code) -> add(Usage(place, RelationKind.NAMES_IN_KOTLIN, timer, UsageEffect.KOTLIN_TO_CHANGE))
                }
            }
        }
        if ((type as? ManagerTypeDefinition)?.kotlinUi?.let(::isUsingMembers) == true) {
            add(Usage(UsagePlace.InModel(RelationPlace.KotlinUi(type.name)), RelationKind.NAMES_IN_KOTLIN, timer, UsageEffect.KOTLIN_TO_CHANGE))
        }
    }
}

/**
 * The Timer [subject] as one subject whichever type it was asked about through: owned by the topmost type of its family,
 * which declares it.
 */
internal fun timerOf(subject: RelationSubject.Timer, types: List<BlockTypeDefinition>, family: ActorTypeFamily) =
    if (types.firstOrNull { it.name == subject.owner } is ActorTypeDefinition) subject.copy(owner = family.rootOf(subject.owner)) else subject

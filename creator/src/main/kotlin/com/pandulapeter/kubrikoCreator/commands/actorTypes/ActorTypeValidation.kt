/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.PropertyNaming
import com.pandulapeter.kubrikoCreator.actors.TIMER_NUMBERS
import com.pandulapeter.kubrikoCreator.actors.actorTypeReference
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import com.pandulapeter.kubrikoCreator.actors.timerNumber
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.workspace.ActorTypesController
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_block_missing
import kubriko.creator.generated.resources.error_command_block_not_allowed
import kubriko.creator.generated.resources.error_command_event_missing
import kubriko.creator.generated.resources.error_command_key_not_allowed
import kubriko.creator.generated.resources.error_command_no_actor_types
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_other_actor_type_not_allowed
import kubriko.creator.generated.resources.error_command_property_missing
import kubriko.creator.generated.resources.error_command_property_unknown
import kubriko.creator.generated.resources.error_command_size_invalid
import kubriko.creator.generated.resources.error_command_timer_invalid
import kubriko.creator.generated.resources.error_command_timer_not_allowed

/**
 * The Actor types of the project, which every Actor type command has validated to exist before it is applied.
 */
internal val CommandContext.requireActorTypes: ActorTypesController get() = checkNotNull(actorTypes)

internal fun CommandContext.requireActorType(name: String) = checkNotNull(actorTypes?.get(name))

internal fun CommandContext.validateActorType(name: String): Rejection? = when {
    actorTypes == null -> Rejection(Res.string.error_command_no_actor_types)
    actorTypes[name] == null -> Rejection(Res.string.error_command_actor_type_missing, name)
    else -> null
}

internal fun CommandContext.validateEvent(actorTypeName: String, eventIndex: Int): Rejection? = validateActorType(actorTypeName)
    ?: if (eventIndex !in requireActorType(actorTypeName).events.indices) Rejection(Res.string.error_command_event_missing, actorTypeName, eventIndex.toString()) else null

internal fun CommandContext.validateBlock(actorTypeName: String, eventIndex: Int, blockIndex: Int): Rejection? = validateEvent(actorTypeName, eventIndex)
    ?: if (blockIndex !in requireActorType(actorTypeName).events[eventIndex].blocks.indices) {
        Rejection(Res.string.error_command_block_missing, actorTypeName, eventIndex.toString(), blockIndex.toString())
    } else {
        null
    }

internal fun CommandContext.validateProperty(actorTypeName: String, propertyName: String): Rejection? = validateActorType(actorTypeName)
    ?: if (requireActorType(actorTypeName).properties.none { it.name == propertyName }) Rejection(Res.string.error_command_property_unknown, actorTypeName, propertyName) else null

internal fun validateSize(width: Float, height: Float) = if (width.isFinite() && height.isFinite() && width > 0f && height > 0f) null else Rejection(Res.string.error_command_size_invalid)

/**
 * Why [event] can't be part of an Actor type with [properties], given the names of the Actor types its collision Event
 * may refer to.
 */
internal fun validateEvent(event: EventDefinition, actorTypeNames: Collection<String>, properties: List<PropertyDefinition>): Rejection? = when {
    event.key != null && event.type != EventType.KEY_PRESSED -> Rejection(Res.string.error_command_key_not_allowed)
    event.otherActorType != null && event.type != EventType.COLLISION -> Rejection(Res.string.error_command_other_actor_type_not_allowed)
    event.timer != null && event.type != EventType.TIMER_FINISHED -> Rejection(Res.string.error_command_timer_not_allowed)
    event.timer != null && event.timer !in TIMER_NUMBERS -> Rejection(Res.string.error_command_timer_invalid)
    event.otherActorType != null && event.otherActorType !in actorTypeNames -> Rejection(Res.string.error_command_actor_type_missing, event.otherActorType)
    else -> event.blocks.firstNotNullOfOrNull { validateBlock(it, event.type, properties, actorTypeNames) }
}

/**
 * Why [block] can't be in an Event of [eventType] of an Actor type with [properties], in a project with Actor types of
 * [actorTypeNames]. A Block may leave its Property or Actor type unchosen, but not name one that doesn't exist.
 */
internal fun validateBlock(block: BlockDefinition, eventType: EventType, properties: List<PropertyDefinition>, actorTypeNames: Collection<String>): Rejection? = when {
    !BlockKind.of(block).isAvailableIn(eventType) -> Rejection(Res.string.error_command_block_not_allowed, block.serialName, eventType.serialName)
    !block.areNumbersFinite -> Rejection(Res.string.error_command_number_invalid)
    block.timerNumber?.let { it !in TIMER_NUMBERS } == true -> Rejection(Res.string.error_command_timer_invalid)
    block.actorTypeReference?.let { it !in actorTypeNames } == true -> Rejection(Res.string.error_command_actor_type_missing, block.actorTypeReference.orEmpty())
    else -> block.propertyReference?.let { (name, kind) ->
        if (name.isNotEmpty() && properties.none { it.name == name && PropertyKind.of(it) == kind }) Rejection(Res.string.error_command_property_missing, name, kind.serialName) else null
    }
}

/**
 * Why [property] can't be one of [properties] (which it replaces, if [replacedName] names one of them).
 */
internal fun validateProperty(property: PropertyDefinition, properties: List<PropertyDefinition>, replacedName: String? = null): Rejection? =
    PropertyNaming.validate(property.name, properties.map { it.name }.filter { it != replacedName })?.let(::Rejection)
        ?: if (property is PropertyDefinition.Number && !property.initialValue.isFinite()) Rejection(Res.string.error_command_number_invalid) else null

internal fun ActorTypeDefinition.validateContents(actorTypeNames: Collection<String>) = validateSize(width, height)
    ?: properties.indices.firstNotNullOfOrNull { index -> validateProperty(properties[index], properties.take(index)) }
    ?: events.firstNotNullOfOrNull { validateEvent(it, actorTypeNames + name, properties) }

private val BlockDefinition.areNumbersFinite
    get() = when (this) {
        is BlockDefinition.SetVelocity -> x.isFinite() && y.isFinite()
        is BlockDefinition.AddToVelocity -> x.isFinite() && y.isFinite()
        is BlockDefinition.JumpTo -> x.isFinite() && y.isFinite()
        is BlockDefinition.If -> when (val condition = condition) {
            is ConditionDefinition.Number -> condition.value.isFinite()
            is ConditionDefinition.Velocity -> condition.value.isFinite()
            is ConditionDefinition.Chance -> condition.percent.isFinite() && condition.percent in 0f..100f
            is ConditionDefinition.Text, is ConditionDefinition.Toggle, is ConditionDefinition.KeyHeld -> true
        }
        is BlockDefinition.Repeat -> times >= 0
        is BlockDefinition.StartTimer -> seconds.isFinite() && seconds >= 0f
        is BlockDefinition.AddActor -> x.isFinite() && y.isFinite()
        is BlockDefinition.MoveTowardsNearest -> speed.isFinite()
        is BlockDefinition.SetNumber -> value.isFinite()
        is BlockDefinition.ChangeNumber -> by.isFinite()
        BlockDefinition.Stop, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther, BlockDefinition.RemoveOther,
        is BlockDefinition.SetText, is BlockDefinition.SetToggle, is BlockDefinition.StopTimer, BlockDefinition.Else, BlockDefinition.End,
        is BlockDefinition.Log, is BlockDefinition.Kotlin -> true
    }

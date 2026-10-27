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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.JsonSchema
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.workspace.ActorTypesController
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_block_missing
import kubriko.creator.generated.resources.error_command_block_not_allowed
import kubriko.creator.generated.resources.error_command_event_missing
import kubriko.creator.generated.resources.error_command_key_not_allowed
import kubriko.creator.generated.resources.error_command_no_actor_types
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_other_actor_type_not_allowed
import kubriko.creator.generated.resources.error_command_size_invalid

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

internal fun validateSize(width: Float, height: Float) = if (width.isFinite() && height.isFinite() && width > 0f && height > 0f) null else Rejection(Res.string.error_command_size_invalid)

/**
 * Why [event] can't be part of an Actor type, given the names of the Actor types its collision Event may refer to.
 */
internal fun validateEvent(event: EventDefinition, actorTypeNames: Collection<String>): Rejection? = when {
    event.key != null && event.type != EventType.KEY_PRESSED -> Rejection(Res.string.error_command_key_not_allowed)
    event.otherActorType != null && event.type != EventType.COLLISION -> Rejection(Res.string.error_command_other_actor_type_not_allowed)
    event.otherActorType != null && event.otherActorType !in actorTypeNames -> Rejection(Res.string.error_command_actor_type_missing, event.otherActorType)
    else -> event.blocks.firstNotNullOfOrNull { validateBlock(it, event.type) }
}

internal fun validateBlock(block: BlockDefinition, eventType: EventType): Rejection? = when {
    !BlockKind.of(block).isAvailableIn(eventType) -> Rejection(Res.string.error_command_block_not_allowed, block.serialName, eventType.serialName)
    !block.areNumbersFinite -> Rejection(Res.string.error_command_number_invalid)
    else -> null
}

internal fun ActorTypeDefinition.validateContents(actorTypeNames: Collection<String>) = validateSize(width, height)
    ?: events.firstNotNullOfOrNull { validateEvent(it, actorTypeNames + name) }

private val BlockDefinition.areNumbersFinite
    get() = when (this) {
        is BlockDefinition.SetVelocity -> x.isFinite() && y.isFinite()
        is BlockDefinition.AddToVelocity -> x.isFinite() && y.isFinite()
        is BlockDefinition.JumpTo -> x.isFinite() && y.isFinite()
        BlockDefinition.Stop, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther, BlockDefinition.RemoveOther, is BlockDefinition.Kotlin -> true
    }

private val EventType.serialName get() = EventType.serializer().descriptor.getElementName(ordinal)

private val BlockDefinition.serialName get() = Json.encodeToJsonElement(BlockDefinition.serializer(), this).jsonObject.getValue(JsonSchema.TYPE_PROPERTY).jsonPrimitive.content

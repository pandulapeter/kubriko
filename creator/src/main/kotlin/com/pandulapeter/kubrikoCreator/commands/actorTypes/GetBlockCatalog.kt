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

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.JsonSchema
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_get_block_catalog

internal object GetBlockCatalog : ReadCommand<NoInput> {
    override val name = "get_block_catalog"
    override val description = "Lists every Event an Actor type or Manager type can react to and every Block an Event can run: what each one does, " +
        "which Events allow which Blocks (in an Actor type; a Manager type has no Blocks that need an Actor), and the properties of each Block."
    override val inputSerializer = NoInput.serializer()
    override val title = Res.string.command_get_block_catalog

    override suspend fun read(context: CommandContext, input: NoInput) = CommandOutcome.Success(catalog)

    @OptIn(ExperimentalSerializationApi::class)
    private val catalog by lazy {
        val eventDescriptor = EventType.serializer().descriptor
        val blockDescriptors = BlockDefinition.serializer().descriptor.getElementDescriptor(1).elementDescriptors.associateBy { it.serialName }
        buildJsonObject {
            putJsonArray("events") {
                EventType.entries.forEach { eventType ->
                    add(buildJsonObject {
                        put("type", eventType.serialName)
                        put("description", eventDescriptor.getElementAnnotations(eventType.ordinal).filterIsInstance<Description>().single().text)
                        put("inActorTypes", eventType.isAvailableIn(BlockTypeKind.ACTOR))
                        put("inManagerTypes", eventType.isAvailableIn(BlockTypeKind.MANAGER))
                        putJsonArray("blocks") { BlockKind.entries.filter { it.isAvailableIn(eventType, BlockTypeKind.ACTOR) }.forEach { add(it.serialName) } }
                    })
                }
            }
            putJsonArray("blocks") {
                BlockKind.entries.forEach { kind ->
                    add(buildJsonObject {
                        put("type", kind.serialName)
                        putJsonArray("allowedIn") { EventType.entries.filter { kind.isAvailableIn(it, BlockTypeKind.ACTOR) }.forEach { add(it.serialName) } }
                        put("needsActor", kind.needsActor)
                        put("schema", JsonSchema.of(blockDescriptors.getValue(kind.serialName)))
                    })
                }
            }
        }
    }
}

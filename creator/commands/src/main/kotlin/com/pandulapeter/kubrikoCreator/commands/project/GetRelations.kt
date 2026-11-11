/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.SimilarNames
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateType
import com.pandulapeter.kubrikoCreator.commands.scenes.validateScene
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.projectRelations
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

public object GetRelations : ReadCommand<GetRelations.Input> {
    override val name: String = "get_relations"
    override val description: String = "Lists what an Actor type, a Manager type, one of their Properties, or a Scene refers to (uses) and what refers to it (usedBy), " +
        "from Kubriko Creator's model of the game: parents, collision Events, Blocks that add Actors, move towards them, go to Scenes, or read and change " +
        "Properties, Log Blocks and texts showing Properties, Kotlin naming types, and Scenes placing Actors. Places are written Type#event.block, with " +
        "indexes from 0 as the other tools take them, and a Scene's count is how many Actors of the type it places. Cheaper than reading every type to find out."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = input.type?.let { CommandTarget.BlockType(it) } ?: input.scene?.let { CommandTarget.Scene(it) }

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        val rejection = when {
            (input.type == null) == (input.scene == null) || (input.property != null && input.type == null) -> Rejection(CommandProblem.RELATIONS_SUBJECT)
            input.type != null -> context.validateType(input.type)
            else -> context.validateScene(input.scene!!)
        }
        rejection?.let { return CommandOutcome.Rejected(it) }
        val relations = context.projectRelations()
        val subject = when {
            input.property != null -> RelationSubject.Property(owner = relations.ownerOfProperty(input.type!!, input.property), name = input.property)
            input.type != null -> RelationSubject.Type(input.type)
            else -> RelationSubject.Scene(input.scene!!)
        }
        if (subject is RelationSubject.Property && context.blockTypes?.get(subject.owner)?.properties?.none { it.name == subject.name } != false) {
            val names = context.blockTypes?.get(subject.owner)?.properties.orEmpty().map { it.name }
            return CommandOutcome.Rejected(
                SimilarNames.rejection(CommandProblem.RELATIONS_PROPERTY_MISSING, CommandProblem.RELATIONS_PROPERTY_MISSING_SIMILAR, input.property!!, names, input.property, input.type!!),
            )
        }
        return CommandOutcome.Success(
            buildJsonObject {
                put("subject", subject.toString())
                if (subject !is RelationSubject.Property) putJsonArray("uses") { relations.from(subject).forEach { add(it.toString()) } }
                putJsonArray("usedBy") { relations.to(subject).forEach { add(it.toString()) } }
            },
        )
    }

    @Serializable
    public data class Input(
        @Description("The name of an Actor type or Manager type, or the type whose Property it is.")
        @SerialName("type") val type: String? = null,
        @Description("The name of a Property of the type, its own or inherited, to list only what refers to that Property.")
        @SerialName("property") val property: String? = null,
        @Description("The name of a Scene, instead of a type: its path in the Scenes folder without .json.")
        @SerialName("scene") val scene: String? = null,
    )
}

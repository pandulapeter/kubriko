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
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.SimilarNames
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateType
import com.pandulapeter.kubrikoCreator.commands.resources.validateExistingResource
import com.pandulapeter.kubrikoCreator.commands.scenes.validateScene
import com.pandulapeter.kubrikoCreator.commands.screenplay.validatePage
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.findUsages
import com.pandulapeter.kubrikoCreator.context.projectRelations
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

public object FindUsages : ReadCommand<FindUsages.Input> {
    override val name: String = "find_usages"
    override val description: String = "Lists every place that refers to one thing (a type, a Property, a Scene, a page, or a sprite, sound, or music file): " +
        "Creator's model, the first Scene, hand-written Kotlin, and the pages mentioning or showing it, as a delete reads them. Places are written " +
        "Type#event.block, indexes from 0; a count in brackets is how many times a place refers to it."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? =
        input.type?.let { CommandTarget.BlockType(it) } ?: input.scene?.let { CommandTarget.Scene(it) } ?: input.page?.let { CommandTarget.Page(it) }

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        val named = listOfNotNull(input.type, input.scene, input.page, input.resource)
        val rejection = when {
            named.size != 1 || (input.property != null && input.type == null) -> Rejection(CommandProblem.USAGES_SUBJECT)
            input.type != null -> context.validateType(input.type)
            input.scene != null -> context.validateScene(input.scene)
            input.page != null -> context.validatePage(input.page)
            else -> context.validateExistingResource(input.resource!!)
        }
        rejection?.let { return CommandOutcome.Rejected(it) }
        val subject = subjectOf(context, input) ?: return CommandOutcome.Rejected(missingProperty(context, input.type!!, input.property!!))
        val usages = context.findUsages(subject)
        return CommandOutcome.Success(
            buildJsonObject {
                put("subject", subject.toString())
                putJsonArray("usages") { usages.forEach { add(it.reference) } }
            },
        )
    }

    private suspend fun subjectOf(context: CommandContext, input: Input): RelationSubject? = when {
        input.property != null -> {
            val owner = context.projectRelations().ownerOfProperty(input.type!!, input.property)
            RelationSubject.Property(owner = owner, name = input.property).takeIf { context.blockTypes?.get(owner)?.properties?.any { it.name == input.property } == true }
        }
        input.type != null -> RelationSubject.Type(input.type)
        input.scene != null -> RelationSubject.Scene(input.scene)
        input.page != null -> RelationSubject.Page(input.page)
        else -> RelationSubject.Resource(input.resource!!)
    }

    private fun missingProperty(context: CommandContext, type: String, property: String): Rejection {
        val names = context.blockTypes?.get(type)?.properties.orEmpty().map { it.name }
        return SimilarNames.rejection(LookupProblem.RELATIONS_PROPERTY_MISSING, LookupProblem.RELATIONS_PROPERTY_MISSING_SIMILAR, property, names, property, type)
    }

    @Serializable
    public data class Input(
        @Description("The name of an Actor type or Manager type, or the type whose Property it is.")
        @SerialName("type") val type: String? = null,
        @Description("The name of a Property of the type, its own or inherited, to list only what refers to that Property.")
        @SerialName("property") val property: String? = null,
        @Description("The name of a Scene, instead of a type: its path in the Scenes folder without .json.")
        @SerialName("scene") val scene: String? = null,
        @Description("The name of a page of the screenplay, instead of a type: its path in the screenplay folder without .md.")
        @SerialName("page") val page: String? = null,
        @Description("A sprite, sound, or music file, instead of a type: its path from composeResources, like drawable/ball.png or files/sounds/hit.wav.")
        @SerialName("resource") val resource: String? = null,
    )
}

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

import com.pandulapeter.kubrikoCreator.actors.withPropertyRemoved
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.DeletingCommand
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.deleteWithUsages
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Removes a Property but keeps its values in the Scenes, which loading ignores, so that undoing this brings them back.
 */
@OptIn(EditorMutation::class)
public object RemoveProperty : DeletingCommand<RemoveProperty.Input> {
    override val name: String = "remove_property"
    override val description: String = "Removes a Property from a block-built Actor type or Manager type, keeping its values in Scenes. " + Descriptions.DELETE_CLEANUP
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun subject(input: Input): RelationSubject = RelationSubject.Property(input.actorType, input.property)

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateProperty(input.actorType, input.property)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        if (!input.keepsUsages) {
            return context.deleteWithUsages(subject(input), Invocation(RemoveProperty, input.copy(keepsUsages = true)))
        }
        val definition = context.requireType(input.actorType)
        val index = definition.properties.indexOfFirst { it.name == input.property }
        context.requireBlockTypes.put(definition.withPropertyRemoved(input.property))
        return Applied(inverse = Invocation(AddProperty, AddProperty.Input(input.actorType, definition.properties[index], index)))
    }

    /**
     * @param keepsUsages Whether it deletes only the Property itself, leaving what refers to it: the last part of the delete, after
     *   [deleteWithUsages] cleaned that up, and what undoes its creation, which leaves what referred to it before as it was.
     */
    @Serializable
    public data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description("The name of the Property.")
        @SerialName("property") val property: String,
        @Transient val keepsUsages: Boolean = false,
    )
}

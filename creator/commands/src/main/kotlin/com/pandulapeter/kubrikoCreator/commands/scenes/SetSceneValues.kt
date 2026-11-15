/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.scenes

import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.ReplaceScenes
import com.pandulapeter.kubrikoCreator.commands.SimilarNames
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.fits
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireManagerType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateManagerType
import com.pandulapeter.kubrikoCreator.scenes.ManagerSceneValues
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public object SetSceneValues : WriteCommand<SetSceneValues.Input> {
    override val name: String = "set_scene_values"
    override val description: String = "Sets the values a Scene gives Properties of a block-built Manager type when it starts (the lives a level " +
        "begins with), or clears some, which then keep the Manager's. Kept as the Scene's first Actor, of the Manager type's name."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateScene(input.scene)
        ?: context.validateManagerType(input.managerType)
        ?: validateValues(context, input)
        ?: input.actorId?.let { actorId ->
            val actors = context.readScene(input.scene)
            if (ManagerSceneValues.indexIn(actors, input.managerType) < 0) validateNewActorId(actorId, actors) else null
        }

    private fun validateValues(context: CommandContext, input: Input): Rejection? {
        val properties = context.requireManagerType(input.managerType).properties
        val names = input.values.map { it.name } + input.cleared
        names.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }?.let { return Rejection(CommandProblem.SCENE_VALUE_NAMED_TWICE, it.key) }
        if (input.values.any { it.number?.isFinite() == false }) return Rejection(CommandProblem.NUMBER_INVALID)
        val state = context.readScene(input.scene).let { actors -> actors.getOrNull(ManagerSceneValues.indexIn(actors, input.managerType)) }?.state
        if (state != null && SceneDocument.stateObject(state) == null) return Rejection(CommandProblem.SCENE_CONTENT_INVALID)
        return names.firstNotNullOfOrNull { name ->
            val property = properties.firstOrNull { it.name == name }
                ?: return@firstNotNullOfOrNull if (name in input.cleared && state != null && name in ManagerSceneValues.valuesOf(state)) {
                    null
                } else {
                    SimilarNames.rejection(
                        LookupProblem.ACTOR_PROPERTY_MISSING,
                        LookupProblem.ACTOR_PROPERTY_MISSING_SIMILAR,
                        name,
                        properties.map { it.name },
                        input.managerType,
                        name,
                    )
                }
            val value = input.values.firstOrNull { it.name == name } ?: return@firstNotNullOfOrNull null
            if (value.toJson()?.fits(PropertyKind.of(property)) == true) null else Rejection(CommandProblem.PROPERTY_VALUE_INVALID, name)
        }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val previousText = checkNotNull(context.scenes.read(input.scene))
        val actors = context.readScene(input.scene)
        val index = ManagerSceneValues.indexIn(actors, input.managerType)
        val previous = actors.getOrNull(index)
        val state = ManagerSceneValues.withValues(
            state = previous?.state,
            values = input.values.associate { it.name to checkNotNull(it.toJson()) },
            cleared = input.cleared,
            order = context.requireManagerType(input.managerType).properties.map { it.name },
        )
        context.writeScene(
            input.scene,
            when {
                state == null -> actors.filterIndexed { actorIndex, _ -> actorIndex != index }
                // First, since Kubriko adds a Scene's Actors in order: the values are given before any Actor's added Events run.
                previous == null -> listOf(SceneActorEntry(input.managerType, state, input.actorId ?: SceneDocument.newId(actors.map { it.id }))) + actors
                else -> actors.mapIndexed { actorIndex, actor -> if (actorIndex == index) actor.copy(state = state) else actor }
            },
        )
        // The Scene as it was, byte for byte, which brings back a value even of a Property removed since.
        return Applied(inverse = Invocation(ReplaceScenes, ReplaceScenes.Input(mapOf(input.scene to previousText))))
    }

    override fun mergeKey(input: Input): Any? = listOf(input.scene, input.managerType, input.values.map { it.name }, input.cleared)

    @Serializable
    public data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
        @Description(Descriptions.MANAGER_TYPE_NAME)
        @SerialName("managerType") val managerType: String,
        @Description("The values to give.")
        @SerialName("values") val values: List<ActorValueChanges.PropertyValue> = emptyList(),
        @Description("The Properties no longer given a value.")
        @SerialName("cleared") val cleared: List<String> = emptyList(),
        @Description("The ID of the Scene's Actor of the values when it has none yet; a new one if left out.")
        @SerialName("actorId") val actorId: String? = null,
    )
}

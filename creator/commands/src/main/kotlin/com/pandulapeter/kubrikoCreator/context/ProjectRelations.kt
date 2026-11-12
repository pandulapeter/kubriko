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
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyPlaceholders
import com.pandulapeter.kubrikoCreator.actors.codegen.SceneSettingsCodeGenerator
import com.pandulapeter.kubrikoCreator.actors.numberPropertyReferences
import com.pandulapeter.kubrikoCreator.actors.propertyReference

/**
 * Every reference between the parts of a game, read from Kubriko Creator's model of it rather than from its code: what
 * an Actor type, a Manager type, a Property, or a Scene refers to, and what refers to it.
 *
 * @param scenes The number of Actors of each type ID that each Scene places, by the Scene's name.
 */
internal class ProjectRelations(
    types: List<BlockTypeDefinition>,
    scenes: Map<String, Map<String, Int>>,
) {
    private val typeNames = types.mapTo(HashSet()) { it.name }
    private val family = ActorTypeFamily(types.filterIsInstance<ActorTypeDefinition>())
    private val declaredProperties = types.associate { type -> type.name to type.properties.mapTo(HashSet()) { it.name } }

    val all: List<Relation> = buildList {
        types.forEach { type -> addRelationsOf(type) }
        scenes.forEach { (scene, counts) ->
            counts.forEach { (typeId, count) ->
                if (typeId != SceneSettingsCodeGenerator.SETTINGS_NAME) add(Relation(RelationPlace.Scene(scene), RelationKind.PLACES, RelationSubject.Type(typeId), count))
            }
        }
    }

    /**
     * What [subject] refers to: for a type, also what its Events and Blocks do.
     */
    fun from(subject: RelationSubject) = all.filter { it.place.owner == subject }

    /**
     * What refers to [subject]: for a type, also to its Properties.
     */
    fun to(subject: RelationSubject) = all.filter { relation ->
        relation.subject == subject || (subject is RelationSubject.Type && (relation.subject as? RelationSubject.Property)?.owner == subject.name)
    }

    /**
     * The type that declares the Property [name] that [typeName] uses as its own: itself or the nearest ancestor that does.
     */
    fun ownerOfProperty(typeName: String, name: String) = (listOf(typeName) + family.ancestorsOf(typeName))
        .firstOrNull { declaredProperties[it]?.contains(name) == true } ?: typeName

    private fun MutableList<Relation>.addRelationsOf(type: BlockTypeDefinition) {
        if (type is ActorTypeDefinition) type.parent?.let { add(Relation(RelationPlace.Type(type.name), RelationKind.INHERITS_FROM, RelationSubject.Type(it))) }
        type.events.forEachIndexed { eventIndex, event ->
            event.otherActorType?.let { add(Relation(RelationPlace.Type(type.name, eventIndex), RelationKind.COLLIDES_WITH, RelationSubject.Type(it))) }
            event.blocks.forEachIndexed { blockIndex, block -> addRelationsOf(type.name, RelationPlace.Type(type.name, eventIndex, blockIndex), block) }
        }
        if (type is ManagerTypeDefinition) {
            type.texts.forEachIndexed { index, text -> addPlaceholders(type.name, RelationPlace.Text(type.name, index), text.text) }
            type.kotlinUi?.let { addKotlinNames(RelationPlace.KotlinUi(type.name), it) }
        }
    }

    private fun MutableList<Relation>.addRelationsOf(typeName: String, place: RelationPlace, block: BlockDefinition) {
        when (block) {
            is BlockDefinition.AddActor -> block.actorType.ifEmpty { null }?.let { add(Relation(place, RelationKind.ADDS, RelationSubject.Type(it))) }
            is BlockDefinition.MoveTowardsNearest -> block.actorType.ifEmpty { null }?.let { add(Relation(place, RelationKind.MOVES_TOWARDS, RelationSubject.Type(it))) }
            is BlockDefinition.GoToScene -> block.scene.ifEmpty { null }?.let { add(Relation(place, RelationKind.GOES_TO, RelationSubject.Scene(it))) }
            is BlockDefinition.Log -> addPlaceholders(typeName, place, block.message)
            is BlockDefinition.Kotlin -> addKotlinNames(place, block.code)
            is BlockDefinition.SetNumber, is BlockDefinition.ChangeNumber, is BlockDefinition.SetText, is BlockDefinition.SetToggle, is BlockDefinition.If,
            -> block.propertyReference?.takeIf { it.name.isNotEmpty() }?.let { reference ->
                val kind = if (block is BlockDefinition.If) RelationKind.READS else RelationKind.CHANGES
                add(Relation(place, kind, propertyOf(typeName, reference.manager, reference.name)))
            }
            is BlockDefinition.SetVelocity, is BlockDefinition.AddToVelocity, BlockDefinition.Stop, BlockDefinition.RunParentEvent,
            is BlockDefinition.JumpTo, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther,
            BlockDefinition.RemoveOther, is BlockDefinition.StartTimer, is BlockDefinition.StopTimer, BlockDefinition.RestartScene,
            is BlockDefinition.PauseGame, is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic, BlockDefinition.StopMusic,
            is BlockDefinition.SetFrame, is BlockDefinition.SetAnimation, BlockDefinition.Else, is BlockDefinition.Repeat, BlockDefinition.End,
            -> Unit
        }
        block.numberPropertyReferences.forEach { reference ->
            if (reference.name.isNotEmpty()) add(Relation(place, RelationKind.READS_AS_NUMBER, propertyOf(typeName, reference.manager, reference.name)))
        }
    }

    private fun MutableList<Relation>.addPlaceholders(typeName: String, place: RelationPlace, text: String) {
        PropertyPlaceholders.find(text).forEach { placeholder -> add(Relation(place, RelationKind.READS, propertyOf(typeName, placeholder.manager, placeholder.name))) }
    }

    private fun MutableList<Relation>.addKotlinNames(place: RelationPlace, code: String) {
        identifier.findAll(code).map { it.value }.filter { it in typeNames }.distinct().forEach { add(Relation(place, RelationKind.NAMES_IN_KOTLIN, RelationSubject.Type(it))) }
    }

    /**
     * The Property called [name] of [manager] (the type's own one, if null) as [typeName] uses it.
     */
    fun propertyOf(typeName: String, manager: String?, name: String) =
        RelationSubject.Property(owner = manager ?: ownerOfProperty(typeName, name), name = name)

    private companion object {
        val identifier = Regex("\\b[A-Z][A-Za-z0-9]*\\b")
    }
}

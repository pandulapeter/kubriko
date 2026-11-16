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
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyPlaceholders
import com.pandulapeter.kubrikoCreator.actors.actorTypeReferences
import com.pandulapeter.kubrikoCreator.actors.codegen.SceneSettingsCodeGenerator
import com.pandulapeter.kubrikoCreator.actors.fixedName
import com.pandulapeter.kubrikoCreator.actors.measuredActorTypes
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import com.pandulapeter.kubrikoCreator.actors.propertyReferences
import com.pandulapeter.kubrikoCreator.actors.valuePropertyReferences

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
    private val managerTypeNames = types.filterIsInstance<ManagerTypeDefinition>().mapTo(HashSet()) { it.name }
    private val family = ActorTypeFamily(types.filterIsInstance<ActorTypeDefinition>())
    private val declaredProperties = types.associate { type -> type.name to type.properties.mapTo(HashSet()) { it.name } }

    val all: List<Relation> = buildList {
        types.forEach { type -> addRelationsOf(type) }
        scenes.forEach { (scene, counts) ->
            counts.forEach { (typeId, count) ->
                when (typeId) {
                    SceneSettingsCodeGenerator.SETTINGS_NAME -> Unit
                    in managerTypeNames -> add(Relation(RelationPlace.Scene(scene), RelationKind.GIVES_VALUES_TO, RelationSubject.Type(typeId)))
                    else -> add(Relation(RelationPlace.Scene(scene), RelationKind.PLACES, RelationSubject.Type(typeId), count))
                }
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
        type.properties.forEach { property ->
            (property as? PropertyDefinition.ActorType)?.initialValue?.ifEmpty { null }?.let { add(Relation(RelationPlace.Type(type.name), RelationKind.STARTS_AS, RelationSubject.Type(it))) }
        }
        type.events.forEachIndexed { eventIndex, event ->
            // The trigger leaves out a field the Event's type doesn't use, which a file edited by hand may still have.
            event.trigger.otherActorType?.let {
                val kind = if (event.type == EventType.COLLISION) RelationKind.COLLIDES_WITH else RelationKind.REACTS_TO
                add(Relation(RelationPlace.Type(type.name, eventIndex), kind, RelationSubject.Type(it)))
            }
            event.blocks.forEachIndexed { blockIndex, block -> addRelationsOf(type.name, RelationPlace.Type(type.name, eventIndex, blockIndex), block) }
        }
        if (type is ManagerTypeDefinition) {
            type.texts.forEachIndexed { index, text ->
                addPlaceholders(type.name, RelationPlace.Text(type.name, index), text.text)
                text.shownWhen?.let { addRelationsOf(type.name, RelationPlace.Text(type.name, index, isCondition = true), it) }
            }
            type.kotlinUi?.let { addKotlinNames(RelationPlace.KotlinUi(type.name), it) }
        }
    }

    private fun MutableList<Relation>.addRelationsOf(typeName: String, place: RelationPlace, block: BlockDefinition) {
        when (block) {
            is BlockDefinition.AddActor -> block.actorType.fixedName?.let { add(Relation(place, RelationKind.ADDS, RelationSubject.Type(it))) }
            is BlockDefinition.MoveTowardsNearest -> block.actorType.fixedName?.let { add(Relation(place, RelationKind.MOVES_TOWARDS, RelationSubject.Type(it))) }
            is BlockDefinition.If -> addCheckedBy(typeName, place, block.condition)
            is BlockDefinition.GoToScene -> block.scene.ifEmpty { null }?.let { add(Relation(place, RelationKind.GOES_TO, RelationSubject.Scene(it))) }
            is BlockDefinition.Log -> addPlaceholders(typeName, place, block.message)
            is BlockDefinition.Kotlin -> addKotlinNames(place, block.code)
            is BlockDefinition.SetNumber, is BlockDefinition.ChangeNumber, is BlockDefinition.SetText, is BlockDefinition.SetToggle, is BlockDefinition.SetColorProperty,
            -> block.propertyReference?.takeIf { it.name.isNotEmpty() }?.let { reference ->
                add(Relation(place, RelationKind.CHANGES, propertyOf(typeName, reference.manager, reference.name)))
            }
            is BlockDefinition.SetVelocity, is BlockDefinition.AddToVelocity, is BlockDefinition.MoveInDirection, is BlockDefinition.MoveWithKeys, BlockDefinition.Stop, BlockDefinition.RunParentEvent,
            is BlockDefinition.JumpTo, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther,
            BlockDefinition.RemoveOther, is BlockDefinition.StartTimer, is BlockDefinition.StopTimer, BlockDefinition.RestartScene,
            is BlockDefinition.PauseGame, is BlockDefinition.PlaySound, is BlockDefinition.PlayMusic, BlockDefinition.StopMusic, BlockDefinition.FollowWithCamera, is BlockDefinition.MoveCamera, is BlockDefinition.ShakeCamera, is BlockDefinition.ZoomCamera,
            is BlockDefinition.SetFrame, is BlockDefinition.SetAnimation, is BlockDefinition.SetColor, is BlockDefinition.SetOpacity,
            is BlockDefinition.ChangeOpacity, is BlockDefinition.SetSize, is BlockDefinition.SetScale, is BlockDefinition.ChangeScale,
            is BlockDefinition.Animate, BlockDefinition.Else, is BlockDefinition.Repeat, BlockDefinition.End,
            -> Unit
        }
        block.valuePropertyReferences.forEach { reference ->
            if (reference.name.isNotEmpty()) add(Relation(place, RelationKind.READS_VALUE, propertyOf(typeName, reference.manager, reference.name)))
        }
    }

    /**
     * What [condition] checks and reads as numbers, at [place] of [typeName].
     */
    private fun MutableList<Relation>.addRelationsOf(typeName: String, place: RelationPlace, condition: ConditionDefinition) {
        addCheckedBy(typeName, place, condition)
        condition.valuePropertyReferences.forEach { reference ->
            if (reference.name.isNotEmpty()) add(Relation(place, RelationKind.READS_VALUE, propertyOf(typeName, reference.manager, reference.name)))
        }
    }

    private fun MutableList<Relation>.addCheckedBy(typeName: String, place: RelationPlace, condition: ConditionDefinition) {
        condition.propertyReferences.forEach { if (it.name.isNotEmpty()) add(Relation(place, RelationKind.READS, propertyOf(typeName, it.manager, it.name))) }
        condition.actorTypeReferences.forEach { if (it.isNotEmpty()) add(Relation(place, RelationKind.COUNTS, RelationSubject.Type(it))) }
        condition.measuredActorTypes.forEach { add(Relation(place, RelationKind.MEASURES, RelationSubject.Type(it))) }
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

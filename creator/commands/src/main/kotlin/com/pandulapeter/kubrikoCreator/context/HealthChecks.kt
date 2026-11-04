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
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition

/**
 * Works out what about a game is worth a look ([HealthFinding]) from its types, the relations between its parts, and
 * its Scenes, as the editor shows it and the AI reads it.
 */
internal object HealthChecks {

    fun of(types: List<BlockTypeDefinition>, relations: ProjectRelations, sceneNames: Collection<String>): List<HealthFinding> {
        val actorTypes = types.filterIsInstance<ActorTypeDefinition>()
        val family = ActorTypeFamily(actorTypes)
        val instantiated = relations.all.filter { it.kind == RelationKind.PLACES || it.kind == RelationKind.ADDS }.mapNotNullTo(HashSet()) { (it.subject as? RelationSubject.Type)?.name }
        fun isPresent(name: String) = name in instantiated || family.descendantsOf(name).any { it in instantiated }
        val read = relations.all.filter { it.kind == RelationKind.READS }.mapNotNullTo(HashSet()) { it.subject as? RelationSubject.Property }
        val kotlin = types.flatMap { type -> type.events.flatMap { it.blocks }.filterIsInstance<BlockDefinition.Kotlin>().map { it.code } + listOfNotNull((type as? ManagerTypeDefinition)?.kotlinUi) }
        val reached = relations.all.filter { it.kind == RelationKind.GOES_TO }.mapNotNullTo(HashSet()) { (it.subject as? RelationSubject.Scene)?.name }
        val declared = types.associate { it.name to it.properties.mapTo(HashSet()) { property -> property.name } }
        fun rootOwnerOf(owner: String, name: String) = (listOf(owner) + family.ancestorsOf(owner)).lastOrNull { declared[it]?.contains(name) == true } ?: owner
        val readByRoot = read.mapTo(HashSet()) { RelationSubject.Property(rootOwnerOf(it.owner, it.name), it.name) }
        val actorTypeNames = actorTypes.mapTo(HashSet()) { it.name }
        val startedTimers = types.associate { type -> type.name to type.events.flatMap { it.blocks }.filterIsInstance<BlockDefinition.StartTimer>().map { it.timer } }
        return buildList {
            actorTypes.filterNot { isPresent(it.name) }.forEach { add(HealthFinding(HealthFinding.Kind.UNUSED_ACTOR_TYPE, RelationSubject.Type(it.name))) }
            if (sceneNames.size > 1) sceneNames.sorted().filterNot { it in reached }.forEach { add(HealthFinding(HealthFinding.Kind.UNREACHED_SCENE, RelationSubject.Scene(it))) }
            types.forEach { type ->
                type.properties.forEach { property ->
                    val pattern = Regex("\\b" + Regex.escape(property.name) + "\\b")
                    if (RelationSubject.Property(rootOwnerOf(type.name, property.name), property.name) !in readByRoot && kotlin.none(pattern::containsMatchIn)) {
                        add(HealthFinding(HealthFinding.Kind.UNREAD_PROPERTY, RelationSubject.Property(type.name, property.name)))
                    }
                }
                val familyNames = if (type is ActorTypeDefinition) listOf(type.name) + family.ancestorsOf(type.name) + family.descendantsOf(type.name) else listOf(type.name)
                val started = familyNames.flatMapTo(HashSet()) { startedTimers[it].orEmpty() }
                type.events.forEach { event ->
                    val timer = event.timer
                    if (event.type == EventType.TIMER_FINISHED && timer != null && timer !in started) {
                        add(HealthFinding(HealthFinding.Kind.TIMER_NEVER_STARTED, RelationSubject.Type(type.name), timer))
                    }
                    val other = event.otherActorType
                    if (event.type == EventType.COLLISION && other != null && other in actorTypeNames && !isPresent(other)) {
                        add(HealthFinding(HealthFinding.Kind.COLLISION_WITH_ABSENT_TYPE, RelationSubject.Type(type.name), other))
                    }
                }
            }
            addAll(KotlinBlockScopeChecks.of(types, family))
            addAll(MissingReferenceChecks.of(types, relations, sceneNames))
        }
    }
}

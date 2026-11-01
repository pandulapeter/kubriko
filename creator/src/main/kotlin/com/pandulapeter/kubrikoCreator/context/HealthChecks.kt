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
        return buildList {
            actorTypes.filterNot { isPresent(it.name) }.forEach { add(HealthFinding(HealthFinding.Kind.UNUSED_ACTOR_TYPE, RelationSubject.Type(it.name))) }
            if (sceneNames.size > 1) sceneNames.sorted().filterNot { it in reached }.forEach { add(HealthFinding(HealthFinding.Kind.UNREACHED_SCENE, RelationSubject.Scene(it))) }
            types.forEach { type ->
                type.properties.forEach { property ->
                    val subject = RelationSubject.Property(type.name, property.name)
                    if (subject !in read && kotlin.none { code -> Regex("\\b" + Regex.escape(property.name) + "\\b").containsMatchIn(code) }) {
                        add(HealthFinding(HealthFinding.Kind.UNREAD_PROPERTY, subject))
                    }
                }
                val familyNames = if (type is ActorTypeDefinition) listOf(type.name) + family.ancestorsOf(type.name) + family.descendantsOf(type.name) else listOf(type.name)
                val started = types.filter { it.name in familyNames }.flatMap { it.events.flatMap { event -> event.blocks } }.filterIsInstance<BlockDefinition.StartTimer>().mapTo(HashSet()) { it.timer }
                type.events.forEach { event ->
                    val timer = event.timer
                    if (event.type == EventType.TIMER_FINISHED && timer != null && timer !in started) {
                        add(HealthFinding(HealthFinding.Kind.TIMER_NEVER_STARTED, RelationSubject.Type(type.name), timer))
                    }
                    val other = event.otherActorType
                    if (event.type == EventType.COLLISION && other != null && actorTypes.any { it.name == other } && !isPresent(other)) {
                        add(HealthFinding(HealthFinding.Kind.COLLISION_WITH_ABSENT_TYPE, RelationSubject.Type(type.name), other))
                    }
                }
            }
        }
    }
}

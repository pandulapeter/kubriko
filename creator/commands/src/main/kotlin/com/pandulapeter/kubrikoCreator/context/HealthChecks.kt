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
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition

/**
 * Works out what about a game is worth a look ([HealthFinding]) from its types, the relations between its parts, and
 * its Scenes, as the editor shows it and the AI reads it, with the Scene the game starts with when the project's settings
 * name it.
 */
internal object HealthChecks {

    fun of(
        types: List<BlockTypeDefinition>,
        relations: ProjectRelations,
        sceneNames: Collection<String>,
        audioFiles: AudioFiles? = null,
        sceneMusic: Map<String, String> = emptyMap(),
        firstScene: String? = null,
    ): List<HealthFinding> {
        val actorTypes = types.filterIsInstance<ActorTypeDefinition>()
        val family = ActorTypeFamily(actorTypes)
        val instantiated = relations.all.filter { it.kind == RelationKind.PLACES || it.kind == RelationKind.ADDS }.mapNotNullTo(HashSet()) { (it.subject as? RelationSubject.Type)?.name }
        fun isPresent(name: String) = name in instantiated || family.descendantsOf(name).any { it in instantiated }
        val read = relations.all.filter { it.kind == RelationKind.READS || it.kind == RelationKind.READS_AS_NUMBER }.mapNotNullTo(HashSet()) { it.subject as? RelationSubject.Property }
        val kotlin = types.flatMap { type -> type.events.flatMap { it.blocks }.filterIsInstance<BlockDefinition.Kotlin>().map { it.code } + listOfNotNull((type as? ManagerTypeDefinition)?.kotlinUi) }
        val reached = relations.all.filter { it.kind == RelationKind.GOES_TO }.mapNotNullTo(HashSet()) { (it.subject as? RelationSubject.Scene)?.name }
        val declared = types.associate { it.name to it.properties.mapTo(HashSet()) { property -> property.name } }
        fun rootOwnerOf(owner: String, name: String) = (listOf(owner) + family.ancestorsOf(owner)).lastOrNull { declared[it]?.contains(name) == true } ?: owner
        val readByRoot = read.mapTo(HashSet()) { RelationSubject.Property(rootOwnerOf(it.owner, it.name), it.name) }
        val actorTypeNames = actorTypes.mapTo(HashSet()) { it.name }
        val startedTimers = types.associate { type -> type.name to type.events.flatMap { it.blocks }.filterIsInstance<BlockDefinition.StartTimer>().map { it.timer } }
        return buildList {
            actorTypes.filterNot { isPresent(it.name) }.forEach { add(HealthFinding(HealthFinding.Kind.UNUSED_ACTOR_TYPE, RelationSubject.Type(it.name))) }
            addAll(sceneFindings(sceneNames, reached, firstScene))
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
                    val other = event.trigger.otherActorType
                    if (other != null && other in actorTypeNames && !isPresent(other)) {
                        val kind = if (event.type == EventType.COLLISION) HealthFinding.Kind.COLLISION_WITH_ABSENT_TYPE else HealthFinding.Kind.REACTION_TO_ABSENT_TYPE
                        add(HealthFinding(kind, RelationSubject.Type(type.name), other))
                    }
                }
                relations.from(RelationSubject.Type(type.name)).filter { it.kind == RelationKind.COUNTS }.mapNotNull { (it.subject as? RelationSubject.Type)?.name }
                    .distinct()
                    .filter { it in actorTypeNames && !isPresent(it) }
                    .forEach { add(HealthFinding(HealthFinding.Kind.COUNT_OF_ABSENT_TYPE, RelationSubject.Type(type.name), it)) }
            }
            addAll(KotlinBlockScopeChecks.of(types, family))
            addAll(MissingReferenceChecks.of(types, relations, sceneNames, audioFiles, sceneMusic))
        }
    }

    /**
     * The Scenes nothing goes to, and the first Scene if it is missing. Without knowing which Scene the game starts with (a
     * project whose build doesn't read its settings, or whose first Scene is missing), any one of them may be, so only a
     * game of several has any, and none names a first Scene.
     */
    private fun sceneFindings(sceneNames: Collection<String>, reached: Set<String>, firstScene: String?) = buildList {
        if (firstScene != null && firstScene !in sceneNames) add(HealthFinding(HealthFinding.Kind.FIRST_SCENE_MISSING, RelationSubject.Scene(firstScene)))
        val knownFirstScene = firstScene?.takeIf { it in sceneNames }
        if (knownFirstScene != null || sceneNames.size > 1) {
            sceneNames.sorted().filterNot { it in reached || it == knownFirstScene }.forEach { add(HealthFinding(HealthFinding.Kind.UNREACHED_SCENE, RelationSubject.Scene(it), knownFirstScene)) }
        }
    }
}

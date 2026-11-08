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
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.KotlinIdentifiers
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.propertyReference

/**
 * Everything that refers to a part of the game, read from Kubriko Creator's model ([ProjectRelations]), the game's first
 * Scene, and hand-written Kotlin: what deleting that part changes, shows, and is refused for. Deletes, their confirmation,
 * and `check_change` all read it, so none can miss what another sees.
 *
 * @param sceneTypeCounts The number of Actors of each type ID that each Scene places, by the Scene's name.
 * @param firstScene The Scene the game starts with, if its settings name one.
 * @param kotlinFiles The text of every hand-written Kotlin file, by its path from the project's root (only those
 *   [readsKotlinFiles] says the usages of a subject need).
 */
internal class ProjectReferences(
    private val types: List<BlockTypeDefinition>,
    sceneTypeCounts: Map<String, Map<String, Int>>,
    private val firstScene: String?,
    private val kotlinFiles: Map<String, String>,
) {
    val relations = ProjectRelations(types, sceneTypeCounts)
    private val family = ActorTypeFamily(types.filterIsInstance<ActorTypeDefinition>())

    /**
     * Every usage of [subject] outside itself, in the order of the project: Kubriko Creator's model first, then the
     * game's settings, then hand-written Kotlin.
     */
    fun usagesOf(subject: RelationSubject): List<Usage> = buildList {
        relations.to(subject)
            .filterNot { subject is RelationSubject.Type && it.place.owner == subject }
            .distinct()
            .forEach { relation -> add(usageOf(relation)) }
        addAll(managerChoiceUsagesOf(subject))
        addAll(kotlinBlockUsagesOf(subject))
        if (subject is RelationSubject.Scene && subject.name == firstScene) {
            add(Usage(UsagePlace.GameSettings, RelationKind.STARTS_WITH, subject, UsageEffect.PREVENTS_DELETING))
        }
        val isNamedIn = fileNaming(subject)
        kotlinFiles.keys.sorted().filter { path -> isNamedIn(kotlinFiles.getValue(path)) }.forEach { path ->
            add(Usage(UsagePlace.KotlinFile(path), RelationKind.NAMES_IN_KOTLIN, subject, UsageEffect.KOTLIN_TO_CHANGE))
        }
    }

    /**
     * The types as deleting what [usages] refer to leaves them: only those it changes, with the Blocks and Events removed
     * and the placeholders turned into text.
     */
    fun typesAfterDeleting(usages: List<Usage>): List<BlockTypeDefinition> = ReferenceCleanup(types, relations).typesAfter(usages)

    /**
     * [usages] as they are once deleting has removed the Blocks and Events it removes: the Kotlin left to change at the
     * place it moved to, which is what the AI is told, and the rest as they were.
     */
    fun usagesAfterDeleting(usages: List<Usage>): List<Usage> = ReferenceCleanup(types, relations).usagesAfter(usages)

    private fun usageOf(relation: Relation): Usage {
        val place = UsagePlace.InModel(relation.place)
        fun usage(effect: UsageEffect, blocksInside: Int = 0) = Usage(place, relation.kind, relation.subject, effect, relation.count, blocksInside)
        return when (relation.kind) {
            RelationKind.INHERITS_FROM -> usage(UsageEffect.PREVENTS_DELETING)
            RelationKind.COLLIDES_WITH -> usage(UsageEffect.EVENT_REMOVED)
            RelationKind.PLACES -> usage(UsageEffect.ACTORS_REMOVED)
            RelationKind.NAMES_IN_KOTLIN -> usage(UsageEffect.KOTLIN_TO_CHANGE)
            RelationKind.ADDS, RelationKind.MOVES_TOWARDS, RelationKind.GOES_TO, RelationKind.CHANGES -> usage(UsageEffect.BLOCK_REMOVED)
            RelationKind.STARTS_WITH -> usage(UsageEffect.PREVENTS_DELETING)
            RelationKind.READS -> when (val block = blockAt(relation.place)) {
                is BlockDefinition.If -> usage(UsageEffect.SECTION_REMOVED, sectionOf(relation.place as RelationPlace.Type).count() - 1)
                null, is BlockDefinition.Log -> usage(UsageEffect.PLACEHOLDER_TO_TEXT)
                else -> error("${block::class.simpleName} doesn't read a Property.")
            }
        }
    }

    /**
     * Blocks that have the Manager type [subject] chosen but none of its Properties yet, which [ProjectRelations] leaves
     * out, since they refer to no Property.
     */
    private fun managerChoiceUsagesOf(subject: RelationSubject) = buildList {
        if (subject !is RelationSubject.Type) return@buildList
        types.forEach { type ->
            if (type.name == subject.name) return@forEach
            type.events.forEachIndexed { eventIndex, event ->
                event.blocks.forEachIndexed { blockIndex, block ->
                    val reference = block.propertyReference ?: return@forEachIndexed
                    if (reference.manager != subject.name || reference.name.isNotEmpty()) return@forEachIndexed
                    val place = RelationPlace.Type(type.name, eventIndex, blockIndex)
                    add(
                        if (block is BlockDefinition.If) {
                            Usage(UsagePlace.InModel(place), RelationKind.READS, subject, UsageEffect.SECTION_REMOVED, blocksInside = sectionOf(place).count() - 1)
                        } else {
                            Usage(UsagePlace.InModel(place), RelationKind.CHANGES, subject, UsageEffect.BLOCK_REMOVED)
                        },
                    )
                }
            }
        }
    }

    /**
     * Kotlin Blocks and Kotlin UI naming a Property, which [ProjectRelations] doesn't follow, since a Property's name is
     * an ordinary word: those of the family of the Actor type declaring it, or, for a Manager type's, those reaching it
     * through its field (and its own).
     */
    private fun kotlinBlockUsagesOf(subject: RelationSubject) = buildList {
        val reaching: (BlockTypeDefinition) -> (KotlinIdentifiers.Occurrence) -> Boolean = when (subject) {
            is RelationSubject.Property -> { type ->
                val ownerType = types.firstOrNull { it.name == subject.owner }
                when {
                    ownerType is ManagerTypeDefinition -> { occurrence ->
                        occurrence.name == subject.name && (occurrence.receiver == BlockScope.managerFieldOf(ownerType.name) || (type.name == ownerType.name && occurrence.receiver == null))
                    }
                    type.name in family.familyOf(subject.owner) -> { occurrence -> occurrence.name == subject.name && occurrence.receiver == null }
                    else -> { _ -> false }
                }
            }
            is RelationSubject.Type -> if (types.any { it is ManagerTypeDefinition && it.name == subject.name }) {
                { _ -> { occurrence -> occurrence.name == BlockScope.managerFieldOf(subject.name) } }
            } else {
                return@buildList
            }
            is RelationSubject.Scene -> {
                addAll(kotlinNamingScene(subject))
                return@buildList
            }
            is RelationSubject.Page, is RelationSubject.Image -> return@buildList
        }
        types.forEach { type ->
            if (subject is RelationSubject.Type && type.name == subject.name) return@forEach
            val isNaming = reaching(type)
            type.events.forEachIndexed { eventIndex, event ->
                event.blocks.forEachIndexed { blockIndex, block ->
                    if (block is BlockDefinition.Kotlin && KotlinIdentifiers.find(block.code).any(isNaming)) {
                        add(Usage(UsagePlace.InModel(RelationPlace.Type(type.name, eventIndex, blockIndex)), RelationKind.NAMES_IN_KOTLIN, subject, UsageEffect.KOTLIN_TO_CHANGE))
                    }
                }
            }
            val kotlinUi = (type as? ManagerTypeDefinition)?.kotlinUi
            if (kotlinUi != null && KotlinIdentifiers.find(kotlinUi).any(isNaming)) {
                add(Usage(UsagePlace.InModel(RelationPlace.KotlinUi(type.name)), RelationKind.NAMES_IN_KOTLIN, subject, UsageEffect.KOTLIN_TO_CHANGE))
            }
        }
    }

    /**
     * Kotlin Blocks and Kotlin UI naming the Scene [subject] in a string, as `goToScene("level-2")` would.
     */
    private fun kotlinNamingScene(subject: RelationSubject.Scene) = buildList {
        val quoted = sceneString(subject.name)
        types.forEach { type ->
            type.events.forEachIndexed { eventIndex, event ->
                event.blocks.forEachIndexed { blockIndex, block ->
                    if (block is BlockDefinition.Kotlin && quoted in block.code) {
                        add(Usage(UsagePlace.InModel(RelationPlace.Type(type.name, eventIndex, blockIndex)), RelationKind.NAMES_IN_KOTLIN, subject, UsageEffect.KOTLIN_TO_CHANGE))
                    }
                }
            }
            if ((type as? ManagerTypeDefinition)?.kotlinUi?.contains(quoted) == true) {
                add(Usage(UsagePlace.InModel(RelationPlace.KotlinUi(type.name)), RelationKind.NAMES_IN_KOTLIN, subject, UsageEffect.KOTLIN_TO_CHANGE))
            }
        }
    }

    private fun sceneString(name: String) = "\"$name\""

    private fun fileNaming(subject: RelationSubject): (String) -> Boolean = when (subject) {
        is RelationSubject.Type -> Regex("\\b" + Regex.escape(subject.name) + "\\b")::containsMatchIn
        is RelationSubject.Scene -> sceneString(subject.name).let { quoted -> { text -> quoted in text } }
        is RelationSubject.Property, is RelationSubject.Page, is RelationSubject.Image -> { _ -> false }
    }

    private fun blockAt(place: RelationPlace) = (place as? RelationPlace.Type)?.let { type ->
        types.firstOrNull { it.name == type.name }?.events?.getOrNull(type.eventIndex ?: return@let null)?.blocks?.getOrNull(type.blockIndex ?: return@let null)
    }

    private fun sectionOf(place: RelationPlace.Type): IntRange {
        val blocks = types.first { it.name == place.name }.events[checkNotNull(place.eventIndex)].blocks
        return sectionOf(blocks, checkNotNull(place.blockIndex))
    }

    companion object {

        /**
         * Whether hand-written Kotlin can name [subject], so that its usages need [kotlinFiles]: a type's class, or a
         * Scene's name in a string, but not a Property, whose name is an ordinary word.
         */
        fun readsKotlinFiles(subject: RelationSubject) = subject !is RelationSubject.Property

        /**
         * The indexes of the if or repeat Block at [index] of [blocks] and of every Block of its section: up to its end
         * Block, or to the end of the Event without one.
         */
        fun sectionOf(blocks: List<BlockDefinition>, index: Int): IntRange {
            val nesting = BlockNesting.of(blocks).first
            val depth = nesting[index].depth
            val end = (index + 1 until blocks.size).firstOrNull { nesting[it].depth == depth && nesting[it].role == BlockNesting.Role.CLOSES } ?: blocks.lastIndex
            return index..end
        }
    }
}

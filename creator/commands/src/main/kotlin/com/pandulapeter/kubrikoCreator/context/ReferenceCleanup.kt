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

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyPlaceholders

/**
 * What deleting something does to the types that use it ([ProjectReferences.typesAfterDeleting]): Blocks and Events
 * that only mean something with it are removed (an Event left empty stays), and placeholders showing its Properties
 * become plain text. Kotlin and Scenes are left to others.
 */
internal class ReferenceCleanup(
    private val types: List<BlockTypeDefinition>,
    private val relations: ProjectRelations,
) {

    fun typesAfter(usages: List<Usage>): List<BlockTypeDefinition> {
        val deletedProperties = usages.mapNotNullTo(HashSet()) { it.subject as? RelationSubject.Property }
        val places = usages.mapNotNull { usage -> ((usage.place as? UsagePlace.InModel)?.place)?.let { it to usage.effect } }
        val byType = places.groupBy { (place, _) -> place.owner }
        return types.mapNotNull { type ->
            val changes = byType[RelationSubject.Type(type.name)] ?: return@mapNotNull null
            cleaned(type, changes, deletedProperties).takeIf { it != type }
        }
    }

    private fun cleaned(type: BlockTypeDefinition, changes: List<Pair<RelationPlace, UsageEffect>>, deletedProperties: Set<RelationSubject.Property>): BlockTypeDefinition {
        fun isDeleted(placeholder: PropertyPlaceholders.Placeholder) = relations.propertyOf(type.name, placeholder.manager, placeholder.name) in deletedProperties
        val removedEvents = HashSet<Int>()
        val removedBlocks = HashMap<Int, MutableSet<Int>>()
        val textsToClean = HashSet<Int>()
        changes.forEach { (place, effect) ->
            when (place) {
                is RelationPlace.Type -> {
                    val eventIndex = place.eventIndex ?: return@forEach
                    val blockIndex = place.blockIndex
                    when (effect) {
                        UsageEffect.EVENT_REMOVED -> removedEvents += eventIndex
                        UsageEffect.BLOCK_REMOVED -> removedBlocks.getOrPut(eventIndex, ::HashSet) += checkNotNull(blockIndex)
                        UsageEffect.SECTION_REMOVED -> removedBlocks.getOrPut(eventIndex, ::HashSet) += ProjectReferences.sectionOf(type.events[eventIndex].blocks, checkNotNull(blockIndex))
                        UsageEffect.PLACEHOLDER_TO_TEXT, UsageEffect.ACTORS_REMOVED, UsageEffect.KOTLIN_TO_CHANGE, UsageEffect.PREVENTS_DELETING -> Unit
                    }
                }
                is RelationPlace.Text -> if (effect == UsageEffect.PLACEHOLDER_TO_TEXT) textsToClean += place.textIndex
                is RelationPlace.KotlinUi, is RelationPlace.Scene -> Unit
            }
        }
        val events = type.events.mapIndexedNotNull { eventIndex, event ->
            if (eventIndex in removedEvents) return@mapIndexedNotNull null
            val removed = removedBlocks[eventIndex].orEmpty()
            event.copy(
                blocks = event.blocks.mapIndexedNotNull { blockIndex, block ->
                    when {
                        blockIndex in removed -> null
                        block is BlockDefinition.Log -> block.copy(message = PropertyPlaceholders.withPlaceholdersAsText(block.message, ::isDeleted))
                        else -> block
                    }
                },
            )
        }
        val withEvents = type.withEvents(events)
        return if (withEvents is ManagerTypeDefinition && textsToClean.isNotEmpty()) {
            withEvents.copy(
                texts = withEvents.texts.mapIndexed { index, text ->
                    if (index in textsToClean) text.copy(text = PropertyPlaceholders.withPlaceholdersAsText(text.text, ::isDeleted)) else text
                },
            )
        } else {
            withEvents
        }
    }
}

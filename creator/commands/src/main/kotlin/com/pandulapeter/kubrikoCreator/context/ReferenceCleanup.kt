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
        val removals = removalsOf(usages)
        return types.mapNotNull { type ->
            val removal = removals[type.name] ?: return@mapNotNull null
            cleaned(type, removal, deletedProperties).takeIf { it != type }
        }
    }

    /**
     * [usages] with the places of the Kotlin left to change moved to where the removed Blocks and Events before them
     * leave them.
     */
    fun usagesAfter(usages: List<Usage>): List<Usage> {
        val removals = removalsOf(usages)
        return usages.map { usage ->
            val place = (usage.place as? UsagePlace.InModel)?.place as? RelationPlace.Type
            val removal = place?.let { removals[it.name] }
            if (usage.effect != UsageEffect.KOTLIN_TO_CHANGE || place?.eventIndex == null || removal == null) return@map usage
            val eventIndex = place.eventIndex
            val moved = place.copy(
                eventIndex = eventIndex - removal.events.count { it < eventIndex },
                blockIndex = place.blockIndex?.let { blockIndex -> blockIndex - removal.blocks[eventIndex].orEmpty().count { it < blockIndex } },
            )
            usage.copy(place = UsagePlace.InModel(moved))
        }
    }

    /**
     * What deleting removes from each type, by its name: Events, Blocks by Event, and the texts whose placeholders change.
     */
    private fun removalsOf(usages: List<Usage>): Map<String, Removal> {
        val removals = HashMap<String, Removal>()
        usages.forEach { usage ->
            val place = (usage.place as? UsagePlace.InModel)?.place ?: return@forEach
            val removal = removals.getOrPut((place.owner as? RelationSubject.Type)?.name ?: return@forEach, ::Removal)
            when (place) {
                is RelationPlace.Type -> {
                    val eventIndex = place.eventIndex ?: return@forEach
                    val blockIndex = place.blockIndex
                    when (usage.effect) {
                        UsageEffect.EVENT_REMOVED -> removal.events += eventIndex
                        UsageEffect.BLOCK_REMOVED -> removal.blocks.getOrPut(eventIndex, ::HashSet) += checkNotNull(blockIndex)
                        UsageEffect.SECTION_REMOVED -> {
                            val blocks = types.first { it.name == place.name }.events[eventIndex].blocks
                            removal.blocks.getOrPut(eventIndex, ::HashSet) += ProjectReferences.sectionOf(blocks, checkNotNull(blockIndex))
                        }
                        UsageEffect.PLACEHOLDER_TO_TEXT, UsageEffect.ACTORS_REMOVED, UsageEffect.KOTLIN_TO_CHANGE, UsageEffect.PREVENTS_DELETING, UsageEffect.MENTION_TO_TEXT -> Unit
                    }
                }
                is RelationPlace.Text -> if (usage.effect == UsageEffect.PLACEHOLDER_TO_TEXT) removal.texts += place.textIndex
                is RelationPlace.KotlinUi, is RelationPlace.Scene -> Unit
            }
        }
        return removals
    }

    private fun cleaned(type: BlockTypeDefinition, removal: Removal, deletedProperties: Set<RelationSubject.Property>): BlockTypeDefinition {
        fun isDeleted(placeholder: PropertyPlaceholders.Placeholder) = relations.propertyOf(type.name, placeholder.manager, placeholder.name) in deletedProperties
        val events = type.events.mapIndexedNotNull { eventIndex, event ->
            if (eventIndex in removal.events) return@mapIndexedNotNull null
            val removed = removal.blocks[eventIndex].orEmpty()
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
        return if (withEvents is ManagerTypeDefinition && removal.texts.isNotEmpty()) {
            withEvents.copy(
                texts = withEvents.texts.mapIndexed { index, text ->
                    if (index in removal.texts) text.copy(text = PropertyPlaceholders.withPlaceholdersAsText(text.text, ::isDeleted)) else text
                },
            )
        } else {
            withEvents
        }
    }

    private class Removal {
        val events = HashSet<Int>()
        val blocks = HashMap<Int, MutableSet<Int>>()
        val texts = HashSet<Int>()
    }
}

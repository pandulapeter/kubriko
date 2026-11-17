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
import com.pandulapeter.kubrikoCreator.actors.ActorTypeValue
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ColorValue
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.MusicValue
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyPlaceholders
import com.pandulapeter.kubrikoCreator.actors.SoundValue
import com.pandulapeter.kubrikoCreator.actors.TextValue
import com.pandulapeter.kubrikoCreator.actors.ToggleValue
import com.pandulapeter.kubrikoCreator.actors.ValueTransform
import com.pandulapeter.kubrikoCreator.actors.actorTypeReference
import com.pandulapeter.kubrikoCreator.actors.measuredActorTypes
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import com.pandulapeter.kubrikoCreator.actors.withValues
import com.pandulapeter.kubrikoCreator.actors.without
import com.pandulapeter.kubrikoCreator.foundation.ColorHex

/**
 * What deleting something does to the types that use it ([ProjectReferences.typesAfterDeleting]):
 * - Blocks and Events that only mean something with it are removed (an Event left empty stays), as are texts shown only
 *   while a condition on it holds.
 * - Placeholders showing its Properties become plain text, and numbers reading them their starting values.
 * - An Actor type drawn as its sprite is drawn as its shape.
 *
 * Kotlin and Scenes are left to others.
 */
internal class ReferenceCleanup(
    private val types: List<BlockTypeDefinition>,
    private val relations: ProjectRelations,
) {

    fun typesAfter(usages: List<Usage>): List<BlockTypeDefinition> {
        val deleted = deletedBy(usages)
        val removals = removalsOf(usages)
        return types.mapNotNull { type ->
            val removal = removals[type.name] ?: return@mapNotNull null
            cleaned(type, removal, deleted).takeIf { it != type }
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
     * What deleting removes from each type, by its name: Events, Blocks by Event, the texts whose placeholders change,
     * and the texts shown only while a condition on it holds.
     */
    private fun removalsOf(usages: List<Usage>): Map<String, Removal> {
        val deleted = deletedBy(usages)
        val removals = HashMap<String, Removal>()
        usages.forEach { usage ->
            val place = (usage.place as? UsagePlace.InModel)?.place ?: return@forEach
            val removal = removals.getOrPut((place.owner as? RelationSubject.Type)?.name ?: return@forEach, ::Removal)
            when (place) {
                is RelationPlace.Type -> {
                    if (usage.effect == UsageEffect.SPRITE_CLEARED) removal.clearsSprite = true
                    if (usage.effect == UsageEffect.STARTING_VALUE_CLEARED) removal.clearsStartingValues = true
                    val eventIndex = place.eventIndex ?: return@forEach
                    val blockIndex = place.blockIndex
                    when (usage.effect) {
                        UsageEffect.EVENT_REMOVED -> removal.events += eventIndex
                        UsageEffect.BLOCK_REMOVED -> removal.blocks.getOrPut(eventIndex, ::HashSet) += checkNotNull(blockIndex)
                        UsageEffect.SECTION_REMOVED -> removal.removeSection(place.name, eventIndex, checkNotNull(blockIndex))
                        UsageEffect.CONDITION_DROPPED -> {
                            // What is left of the or with everything deleted, which one subject's usage can't know.
                            val block = types.first { it.name == place.name }.events[eventIndex].blocks[checkNotNull(blockIndex)] as BlockDefinition.If
                            val condition = relations.conditionWithout(block.condition, place.name, deleted.properties, deleted.types)
                            if (condition == null) removal.removeSection(place.name, eventIndex, blockIndex) else removal.conditions[eventIndex to blockIndex] = condition
                        }
                        UsageEffect.PLACEHOLDER_TO_TEXT, UsageEffect.VALUE_TO_FIXED, UsageEffect.ACTORS_REMOVED, UsageEffect.SCENE_VALUES_REMOVED, UsageEffect.STARTING_VALUE_CLEARED, UsageEffect.SAVED_REFERENCE_CLEARED, UsageEffect.TEXT_REMOVED, UsageEffect.KOTLIN_TO_CHANGE, UsageEffect.PREVENTS_DELETING, UsageEffect.MENTION_TO_TEXT,
                        UsageEffect.SPRITE_CLEARED, UsageEffect.FONT_CLEARED, UsageEffect.MUSIC_CLEARED, UsageEffect.IMAGE_COPIED, UsageEffect.DELETED_WITH_FOLDER,
                        -> Unit
                    }
                }
                is RelationPlace.Text -> when (usage.effect) {
                    UsageEffect.PLACEHOLDER_TO_TEXT -> removal.texts += place.textIndex
                    UsageEffect.TEXT_REMOVED -> removal.removedTexts += place.textIndex
                    UsageEffect.FONT_CLEARED -> removal.clearedFonts += place.textIndex
                    UsageEffect.CONDITION_DROPPED -> {
                        val text = (types.first { it.name == place.manager } as ManagerTypeDefinition).texts[place.textIndex]
                        val condition = relations.conditionWithout(checkNotNull(text.shownWhen), place.manager, deleted.properties, deleted.types)
                        if (condition == null) removal.removedTexts += place.textIndex else removal.textConditions[place.textIndex] = condition
                    }
                    else -> Unit
                }
                is RelationPlace.KotlinUi, is RelationPlace.Scene -> Unit
            }
        }
        return removals
    }

    private fun cleaned(type: BlockTypeDefinition, removal: Removal, deleted: Deleted): BlockTypeDefinition {
        fun isDeleted(placeholder: PropertyPlaceholders.Placeholder) = relations.propertyOf(type.name, placeholder.manager, placeholder.name) in deleted.properties
        val fixing = fixingDeletedValues(type.name, deleted, isOpaque = false)
        val fixingOpaque = fixingDeletedValues(type.name, deleted, isOpaque = true)
        val events = type.events.mapIndexedNotNull { eventIndex, event ->
            if (eventIndex in removal.events) return@mapIndexedNotNull null
            val removed = removal.blocks[eventIndex].orEmpty()
            event.copy(
                blocks = event.blocks.mapIndexedNotNull { blockIndex, block ->
                    when {
                        blockIndex in removed -> null
                        block is BlockDefinition.If && (eventIndex to blockIndex) in removal.conditions -> block.copy(condition = removal.conditions.getValue(eventIndex to blockIndex).withValues(fixing))
                        block is BlockDefinition.Log -> block.copy(message = PropertyPlaceholders.withPlaceholdersAsText(block.message, ::isDeleted))
                        else -> block.withValues(if (block.takesOnlyOpaqueColors) fixingOpaque else fixing)
                    }
                },
            )
        }
        val withEvents = type.withEvents(events).let { if (removal.clearsSprite && it is ActorTypeDefinition) it.copy(sprite = null) else it }.let { changed ->
            if (!removal.clearsStartingValues) {
                changed
            } else {
                changed.withProperties(
                    changed.properties.map { property ->
                        when {
                            property is PropertyDefinition.ActorType && property.initialValue in deleted.types -> property.copy(initialValue = "")
                            property is PropertyDefinition.AudioFile && property.initialValue in deleted.files -> property.withInitialValue("")
                            else -> property
                        }
                    },
                )
            }
        }
        return if (withEvents is ManagerTypeDefinition && withEvents.texts.isNotEmpty()) {
            withEvents.copy(
                texts = withEvents.texts.mapIndexedNotNull { index, text ->
                    if (index in removal.removedTexts) return@mapIndexedNotNull null
                    text.copy(
                        text = if (index in removal.texts) PropertyPlaceholders.withPlaceholdersAsText(text.text, ::isDeleted) else text.text,
                        shownWhen = (removal.textConditions[index] ?: text.shownWhen)?.withValues(fixing),
                        fontFile = text.fontFile.takeIf { index !in removal.clearedFonts },
                    )
                },
            )
        } else {
            withEvents
        }
    }

    /**
     * Turns every value of a type [typeName] that reads a Property that is deleted (or one of a Manager type that is)
     * into that Property's starting value, or its kind's default when no type declares it; a color into an opaque one when
     * [isOpaque], for the Blocks taking only those.
     */
    private fun fixingDeletedValues(typeName: String, deleted: Deleted, isOpaque: Boolean) = object : ValueTransform {
        fun definitionRead(manager: String?, property: String) = deletedPropertyRead(typeName, manager, property, deleted)

        override fun number(value: NumberValue) = (value as? NumberValue.Property)?.let { definitionRead(it.manager, it.property) }
            ?.let { NumberValue.Fixed((it.definition as? PropertyDefinition.Number)?.initialValue ?: 0f) } ?: value

        override fun color(value: ColorValue) = (value as? ColorValue.Property)?.let { definitionRead(it.manager, it.property) }
            ?.let { (it.definition as? PropertyDefinition.Color)?.initialValue ?: PropertyDefinition.Color(value.property).initialValue }
            ?.let { ColorValue.Fixed(if (isOpaque) ColorHex.opaque(it) else it) } ?: value

        override fun actorType(value: ActorTypeValue) = (value as? ActorTypeValue.Property)?.let { definitionRead(it.manager, it.property) }
            ?.let { ActorTypeValue.Fixed((it.definition as? PropertyDefinition.ActorType)?.initialValue.orEmpty()) } ?: value

        override fun text(value: TextValue) = (value as? TextValue.Property)?.let { definitionRead(it.manager, it.property) }
            ?.let { TextValue.Fixed((it.definition as? PropertyDefinition.Text)?.initialValue.orEmpty()) } ?: value

        override fun toggle(value: ToggleValue) = (value as? ToggleValue.Property)?.let { definitionRead(it.manager, it.property) }
            ?.let { ToggleValue.Fixed((it.definition as? PropertyDefinition.Toggle)?.initialValue ?: false) } ?: value

        override fun sound(value: SoundValue) = (value as? SoundValue.Property)?.let { definitionRead(it.manager, it.property) }
            ?.let { SoundValue.Fixed((it.definition as? PropertyDefinition.Sound)?.initialValue.orEmpty()) } ?: value

        override fun music(value: MusicValue) = (value as? MusicValue.Property)?.let { definitionRead(it.manager, it.property) }
            ?.let { MusicValue.Fixed((it.definition as? PropertyDefinition.Music)?.initialValue.orEmpty()) } ?: value
    }

    /**
     * The Property [name] of [manager] (the type's own one, if null) that [typeName] reads, if it, or that Manager type,
     * is deleted.
     */
    private fun deletedPropertyRead(typeName: String, manager: String?, name: String, deleted: Deleted): DeletedProperty? {
        val property = relations.propertyOf(typeName, manager, name)
        if (property !in deleted.properties && manager !in deleted.types) return null
        return DeletedProperty(types.firstOrNull { it.name == property.owner }?.properties?.firstOrNull { it.name == property.name })
    }

    /**
     * @param definition The deleted Property, or null when no type declares it (one named but never made).
     */
    private class DeletedProperty(val definition: PropertyDefinition?)

    /**
     * Whether [this] Block's colors are opaque, the opacity Blocks setting how see-through the Actor is.
     */
    private val BlockDefinition.takesOnlyOpaqueColors get() = this is BlockDefinition.SetColor || this is BlockDefinition.Animate

    private class Deleted(
        val properties: Set<RelationSubject.Property>,
        val types: Set<String>,
        val files: Set<String>,
    )

    private fun deletedBy(usages: List<Usage>) = Deleted(
        properties = usages.mapNotNullTo(HashSet()) { it.subject as? RelationSubject.Property },
        types = usages.mapNotNullTo(HashSet()) { (it.subject as? RelationSubject.Type)?.name },
        files = usages.mapNotNullTo(HashSet()) { (it.subject as? RelationSubject.Resource)?.path },
    )

    private fun Removal.removeSection(typeName: String, eventIndex: Int, blockIndex: Int) {
        val blocks = types.first { it.name == typeName }.events[eventIndex].blocks
        this.blocks.getOrPut(eventIndex, ::HashSet) += BlockNesting.sectionOf(blocks, blockIndex)
    }

    private class Removal {
        val events = HashSet<Int>()
        val blocks = HashMap<Int, MutableSet<Int>>()

        /**
         * The conditions of if Blocks, by Event and Block, that lose what deleting drops from them.
         */
        val conditions = HashMap<Pair<Int, Int>, ConditionDefinition>()
        val texts = HashSet<Int>()
        val removedTexts = HashSet<Int>()

        /**
         * The texts whose font file is deleted, which are drawn in their family instead.
         */
        val clearedFonts = HashSet<Int>()
        val textConditions = HashMap<Int, ConditionDefinition>()
        var clearsSprite = false
        var clearsStartingValues = false
    }
}

/**
 * [condition] of the type [typeName] without the conditions that check what is deleted ([ConditionDefinition.without]): a
 * Property of [properties] or of a Manager type of [types], or the count of, or the distance to, an Actor type of [types].
 */
internal fun ProjectRelations.conditionWithout(
    condition: ConditionDefinition,
    typeName: String,
    properties: Set<RelationSubject.Property>,
    types: Set<String>,
) = condition.without { part ->
    val reference = part.propertyReference
    (reference != null && (reference.manager in types || propertyOf(typeName, reference.manager, reference.name) in properties)) || part.actorTypeReference in types ||
        part.measuredActorTypes.any { it in types }
}

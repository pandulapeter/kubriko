/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * The Property [this] condition checks, if it checks one.
 */
public val ConditionDefinition.propertyReference: PropertyReference?
    get() = when (this) {
        is ConditionDefinition.Number -> PropertyReference(property, PropertyKind.NUMBER, manager)
        is ConditionDefinition.Text -> PropertyReference(property, PropertyKind.TEXT, manager)
        is ConditionDefinition.Toggle -> PropertyReference(property, PropertyKind.TOGGLE, manager)
        is ConditionDefinition.KeyHeld, is ConditionDefinition.Velocity, is ConditionDefinition.ActorCount, is ConditionDefinition.Distance, is ConditionDefinition.Chance,
        is ConditionDefinition.Paused, is ConditionDefinition.Combined,
        -> null
    }

/**
 * The simple conditions [this] one is made of: itself, or those a combined one holds, however deep.
 */
public val ConditionDefinition.leaves: List<ConditionDefinition>
    get() = if (this is ConditionDefinition.Combined) conditions.flatMap { it.leaves } else listOf(this)

/**
 * [this] condition without the conditions [isGone] picks among it and those it combines, or null when it then means
 * nothing: a picked condition means nothing, and so does an and combining one, while an or still holds whenever another of
 * its conditions does, so it only loses that one (and means nothing once it has none left).
 */
public fun ConditionDefinition.without(isGone: (ConditionDefinition) -> Boolean): ConditionDefinition? {
    if (this !is ConditionDefinition.Combined) return takeUnless(isGone)
    val parts = conditions.map { it.without(isGone) }
    return when {
        isAll -> if (parts.any { it == null }) null else copy(conditions = parts.filterNotNull())
        parts.all { it == null } && conditions.isNotEmpty() -> null
        else -> copy(conditions = parts.filterNotNull())
    }
}

/**
 * Every Property [this] condition, or a condition it combines, checks.
 */
public val ConditionDefinition.propertyReferences: List<PropertyReference>
    get() = leaves.mapNotNull { it.propertyReference }

/**
 * [this] condition checking the Property called [name] of [manager] (the type's own one, if null) instead, if it checks
 * a Property.
 */
public fun ConditionDefinition.withPropertyReference(manager: String?, name: String): ConditionDefinition = when (this) {
    is ConditionDefinition.Number -> copy(manager = manager, property = name)
    is ConditionDefinition.Text -> copy(manager = manager, property = name)
    is ConditionDefinition.Toggle -> copy(manager = manager, property = name)
    is ConditionDefinition.KeyHeld, is ConditionDefinition.Velocity, is ConditionDefinition.ActorCount, is ConditionDefinition.Distance, is ConditionDefinition.Chance,
    is ConditionDefinition.Paused, is ConditionDefinition.Combined,
    -> this
}

/**
 * The Actor type [this] condition counts, if it counts one (chosen or not).
 */
public val ConditionDefinition.actorTypeReference: String?
    get() = (this as? ConditionDefinition.ActorCount)?.actorType

/**
 * Every Actor type [this] condition, or a condition it combines, counts (chosen or not).
 */
public val ConditionDefinition.actorTypeReferences: List<String>
    get() = leaves.mapNotNull { it.actorTypeReference }

/**
 * Every Actor type [this] condition, or a condition it combines, measures the distance to by its name, of those chosen.
 */
public val ConditionDefinition.measuredActorTypes: List<String>
    get() = leaves.mapNotNull { (it as? ConditionDefinition.Distance)?.actorType?.fixedName }

/**
 * [this] condition with the Actor type it counts or measures the distance to renamed from [oldName] to [newName], if it
 * is that one.
 */
internal fun ConditionDefinition.withActorTypeRenamed(oldName: String, newName: String): ConditionDefinition = when {
    this is ConditionDefinition.Combined -> copy(conditions = conditions.map { it.withActorTypeRenamed(oldName, newName) })
    this is ConditionDefinition.ActorCount && actorType == oldName -> copy(actorType = newName)
    else -> withValues(
        object : ValueTransform {
            override fun actorType(value: ActorTypeValue) = value.withActorTypeRenamed(oldName, newName)
        },
    )
}

/**
 * [this] condition with the Property of [manager] (the type's own one, if null) that it checks or reads as a value
 * renamed from [oldName] to [newName].
 */
internal fun ConditionDefinition.withPropertyRenamed(oldName: String, newName: String, manager: String?): ConditionDefinition = when {
    this is ConditionDefinition.Combined -> copy(conditions = conditions.map { it.withPropertyRenamed(oldName, newName, manager) })
    propertyReference?.let { it.manager == manager && it.name == oldName } == true -> withPropertyReference(manager, newName).withValues(propertyRenaming(oldName, newName, manager))
    else -> withValues(propertyRenaming(oldName, newName, manager))
}

/**
 * [this] condition with the Properties of the Manager type [oldName] it checks or reads taken from [newName] instead.
 */
internal fun ConditionDefinition.withManagerRenamed(oldName: String, newName: String): ConditionDefinition = when {
    this is ConditionDefinition.Combined -> copy(conditions = conditions.map { it.withManagerRenamed(oldName, newName) })
    propertyReference?.manager == oldName -> withPropertyReference(newName, propertyReference!!.name).withValues(managerRenaming(oldName, newName))
    else -> withValues(managerRenaming(oldName, newName))
}

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
        is ConditionDefinition.KeyHeld, is ConditionDefinition.Velocity, is ConditionDefinition.ActorCount, is ConditionDefinition.Chance, is ConditionDefinition.Paused,
        is ConditionDefinition.Combined,
        -> null
    }

/**
 * The simple conditions [this] one is made of: itself, or those a combined one holds, however deep.
 */
public val ConditionDefinition.leaves: List<ConditionDefinition>
    get() = if (this is ConditionDefinition.Combined) conditions.flatMap { it.leaves } else listOf(this)

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
    is ConditionDefinition.KeyHeld, is ConditionDefinition.Velocity, is ConditionDefinition.ActorCount, is ConditionDefinition.Chance, is ConditionDefinition.Paused,
    is ConditionDefinition.Combined,
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
 * [this] condition with the Actor type it counts renamed from [oldName] to [newName], if it is that one.
 */
internal fun ConditionDefinition.withActorTypeRenamed(oldName: String, newName: String): ConditionDefinition = when {
    this is ConditionDefinition.ActorCount && actorType == oldName -> copy(actorType = newName)
    this is ConditionDefinition.Combined -> copy(conditions = conditions.map { it.withActorTypeRenamed(oldName, newName) })
    else -> this
}

/**
 * Every number [this] condition takes: the same ones [withNumberValues] replaces.
 */
public val ConditionDefinition.numberValues: List<NumberValue>
    get() = when (this) {
        is ConditionDefinition.Number -> listOf(value)
        is ConditionDefinition.Velocity -> listOf(value)
        is ConditionDefinition.ActorCount -> listOf(value)
        is ConditionDefinition.Chance -> listOf(percent)
        is ConditionDefinition.Combined -> conditions.flatMap { it.numberValues }
        is ConditionDefinition.Text, is ConditionDefinition.Toggle, is ConditionDefinition.KeyHeld, is ConditionDefinition.Paused -> emptyList()
    }

/**
 * The Properties [this] condition reads as numbers ([NumberValue.Property]), with or without a name chosen yet.
 */
public val ConditionDefinition.numberPropertyReferences: List<PropertyReference>
    get() = numberValues.mapNotNull { it.propertyReference }

/**
 * [this] condition with each number it takes replaced by what [transform] makes of it.
 */
public fun ConditionDefinition.withNumberValues(transform: (NumberValue) -> NumberValue): ConditionDefinition = when (this) {
    is ConditionDefinition.Number -> copy(value = transform(value))
    is ConditionDefinition.Velocity -> copy(value = transform(value))
    is ConditionDefinition.ActorCount -> copy(value = transform(value))
    is ConditionDefinition.Chance -> copy(percent = transform(percent))
    is ConditionDefinition.Combined -> copy(conditions = conditions.map { it.withNumberValues(transform) })
    is ConditionDefinition.Text, is ConditionDefinition.Toggle, is ConditionDefinition.KeyHeld, is ConditionDefinition.Paused -> this
}

/**
 * [this] condition with the Property of [manager] (the type's own one, if null) that it checks or reads as a value
 * renamed from [oldName] to [newName].
 */
internal fun ConditionDefinition.withPropertyRenamed(oldName: String, newName: String, manager: String?): ConditionDefinition = when {
    this is ConditionDefinition.Combined -> copy(conditions = conditions.map { it.withPropertyRenamed(oldName, newName, manager) })
    propertyReference?.let { it.manager == manager && it.name == oldName } == true -> withPropertyReference(manager, newName)
    else -> this
}.withNumberValues { it.withPropertyRenamed(oldName, newName, manager) }

/**
 * [this] condition with the Properties of the Manager type [oldName] it checks or reads taken from [newName] instead.
 */
internal fun ConditionDefinition.withManagerRenamed(oldName: String, newName: String): ConditionDefinition = when {
    this is ConditionDefinition.Combined -> copy(conditions = conditions.map { it.withManagerRenamed(oldName, newName) })
    propertyReference?.manager == oldName -> withPropertyReference(newName, propertyReference!!.name)
    else -> this
}.withNumberValues { it.withManagerRenamed(oldName, newName) }

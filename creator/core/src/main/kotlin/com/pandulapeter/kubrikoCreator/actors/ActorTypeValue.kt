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

import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The Actor type a Block works with: a type chosen by its name, or the type an Actor type reference Property names. Every
 * field that can take either is one, as [NumberValue] is for numbers and [ColorValue] for colors.
 *
 * A fixed type is written as its bare name, as those fields were before they could be anything else, and a Property's
 * as an object naming its kind in `type` ([ActorTypeValueSerializer]).
 */
@Serializable(with = ActorTypeValueSerializer::class)
public sealed interface ActorTypeValue {

    /**
     * The same Actor type every time, by its name (empty until one is chosen).
     */
    public data class Fixed(val name: String) : ActorTypeValue

    @Serializable
    @SerialName("property")
    @Description("The Actor type an Actor type reference Property names when the Block runs.")
    public data class Property(
        @Description("The name of an Actor type reference Property of the type itself, or of the Manager type in manager.")
        @SerialName("property") val property: String = "",
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : ActorTypeValue

    public companion object {
        internal const val SUMMARY = "An Actor type's name, or a property Actor type value: {\"type\": \"property\", \"property\": name, \"manager\": its Manager type or left out}."
    }
}

/**
 * The Actor type reference Property [this] value reads, if it reads one.
 */
public val ActorTypeValue.propertyReference: PropertyReference?
    get() = (this as? ActorTypeValue.Property)?.let { PropertyReference(it.property, PropertyKind.ACTOR_TYPE, it.manager) }

/**
 * The name of the Actor type [this] value always is, or null if it reads a Property (or none is chosen yet).
 */
public val ActorTypeValue.fixedName: String?
    get() = (this as? ActorTypeValue.Fixed)?.name?.ifEmpty { null }

internal fun ActorTypeValue.withPropertyRenamed(oldName: String, newName: String, manager: String?) =
    if (this is ActorTypeValue.Property && this.manager == manager && property == oldName) copy(property = newName) else this

internal fun ActorTypeValue.withManagerRenamed(oldName: String, newName: String) = if (this is ActorTypeValue.Property && manager == oldName) copy(manager = newName) else this

internal fun ActorTypeValue.withActorTypeRenamed(oldName: String, newName: String) = if (this is ActorTypeValue.Fixed && name == oldName) ActorTypeValue.Fixed(newName) else this

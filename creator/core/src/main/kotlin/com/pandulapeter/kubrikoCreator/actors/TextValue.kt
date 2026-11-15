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
 * A text a Block or a condition takes: a fixed text, or the value of a text Property, as [NumberValue] is for numbers.
 *
 * A fixed text is written as a bare string, as those fields were before they could be anything else, and a Property's as
 * an object naming its kind in `type` ([TextValueSerializer]).
 */
@Serializable(with = TextValueSerializer::class)
public sealed interface TextValue {

    /**
     * The same text every time.
     */
    public data class Fixed(val text: String) : TextValue

    @Serializable
    @SerialName("property")
    @Description("The value of a text Property, of the type itself or of a Manager type, as it is when the Block runs.")
    public data class Property(
        @Description("The name of one of the text Properties of the type itself, or of the Manager type in manager.")
        @SerialName("property") val property: String = "",
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : TextValue

    public companion object {
        internal const val SUMMARY = "A text, or a property text value: {\"type\": \"property\", \"property\": name, \"manager\": its Manager type or left out}."
    }
}

/**
 * The text Property [this] value reads, if it reads one.
 */
public val TextValue.propertyReference: PropertyReference?
    get() = (this as? TextValue.Property)?.let { PropertyReference(it.property, PropertyKind.TEXT, it.manager) }

/**
 * [this] value reading the Property [newName] instead, if it reads [oldName] of [manager] (the type's own one, if null).
 */
internal fun TextValue.withPropertyRenamed(oldName: String, newName: String, manager: String?) =
    if (this is TextValue.Property && this.manager == manager && property == oldName) copy(property = newName) else this

/**
 * [this] value reading its Property from the Manager type [newName] instead, if it reads one of [oldName].
 */
internal fun TextValue.withManagerRenamed(oldName: String, newName: String) = if (this is TextValue.Property && manager == oldName) copy(manager = newName) else this

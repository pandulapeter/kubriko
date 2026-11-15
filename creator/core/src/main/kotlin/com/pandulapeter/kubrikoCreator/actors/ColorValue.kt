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
 * A color a Block takes: a fixed color, or the value of a color Property. Every color field of every Block is one, so a
 * new kind of color reaches them all at once, as [NumberValue] does for numbers.
 *
 * A fixed color is written as a bare `"#RRGGBB"` (`"#AARRGGBB"` when it isn't opaque), as every color field was before it
 * could be anything else, and a Property's as an object naming its kind in `type` ([ColorValueSerializer]).
 */
@Serializable(with = ColorValueSerializer::class)
public sealed interface ColorValue {

    /**
     * The same color every time, as `0xAARRGGBB`.
     */
    public data class Fixed(val argb: Long) : ColorValue

    @Serializable
    @SerialName("property")
    @Description("The value of a color Property, of the type itself or of a Manager type, as it is when the Block runs.")
    public data class Property(
        @Description("The name of one of the color Properties of the type itself, or of the Manager type in manager.")
        @SerialName("property") val property: String = "",
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : ColorValue

    public companion object {
        internal const val SUMMARY = "A color, as #RRGGBB, or a property color value: {\"type\": \"property\", \"property\": name, \"manager\": its Manager type or left out}."
    }
}

/**
 * The color Property [this] value reads, if it reads one.
 */
public val ColorValue.propertyReference: PropertyReference?
    get() = (this as? ColorValue.Property)?.let { PropertyReference(it.property, PropertyKind.COLOR, it.manager) }

/**
 * The fixed color of [this] value, or null if it reads a Property.
 */
public val ColorValue.fixedArgb: Long?
    get() = (this as? ColorValue.Fixed)?.argb

/**
 * [this] value reading the Property [newName] instead, if it reads [oldName] of [manager] (the type's own one, if null).
 */
internal fun ColorValue.withPropertyRenamed(oldName: String, newName: String, manager: String?) =
    if (this is ColorValue.Property && this.manager == manager && property == oldName) copy(property = newName) else this

/**
 * [this] value reading its Property from the Manager type [newName] instead, if it reads one of [oldName].
 */
internal fun ColorValue.withManagerRenamed(oldName: String, newName: String) = if (this is ColorValue.Property && manager == oldName) copy(manager = newName) else this

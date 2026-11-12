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
 * A number a Block takes: a fixed number, a random number between two, or the value of a number Property. Every number
 * field of every Block is one, so any of them can be any of these, worked out each time the Block runs.
 *
 * A fixed number is written as a bare JSON number, as every number field was before it could be anything else, and the
 * others as objects naming their kind in `type` ([NumberValueSerializer]).
 */
@Serializable(with = NumberValueSerializer::class)
public sealed interface NumberValue {

    /**
     * The same number every time.
     */
    public data class Fixed(val value: Float) : NumberValue

    @Serializable
    @SerialName("random")
    @Description("A random number between from and to (both included), drawn anew each time the Block runs.")
    public data class Random(
        @Description("The smallest number it can be.")
        @SerialName("from") val from: Float = 0f,
        @Description("The largest number it can be.")
        @SerialName("to") val to: Float = 1f,
        @Description("True for whole numbers only. A field that only takes whole numbers draws whole numbers either way.")
        @SerialName("isWhole") val isWhole: Boolean = false,
    ) : NumberValue

    @Serializable
    @SerialName("property")
    @Description("The value of a number Property, of the type itself or of a Manager type, as it is when the Block runs.")
    public data class Property(
        @Description("The name of one of the number Properties of the type itself, or of the Manager type in manager.")
        @SerialName("property") val property: String = "",
        @Description(MANAGER_DESCRIPTION)
        @SerialName("manager") val manager: String? = null,
    ) : NumberValue

    public companion object {

        /**
         * How a schema that doesn't list the kinds of numbers names them, after the field's own description.
         */
        internal const val SUMMARY = "A number, or a random or property number value (get_block_catalog's numberValue describes them)."
    }
}

/**
 * The Property [this] value reads, if it reads one.
 */
public val NumberValue.propertyReference: PropertyReference?
    get() = (this as? NumberValue.Property)?.let { PropertyReference(it.property, PropertyKind.NUMBER, it.manager) }

/**
 * Whether every number [this] value names is finite: one that isn't would generate code that doesn't compile.
 */
public val NumberValue.isFinite: Boolean
    get() = when (this) {
        is NumberValue.Fixed -> value.isFinite()
        is NumberValue.Random -> from.isFinite() && to.isFinite()
        is NumberValue.Property -> true
    }

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
 * A number a Block takes: a fixed number, a random number between two, the value of a number Property, where an edge of
 * the Scene is, or a random number between two edges. Every number field of every Block is one, so any of them can be any
 * of these, worked out each time the Block runs.
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
    @Description("A random number from from up to to, drawn anew each time the Block runs; a whole one can also be to itself.")
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

    @Serializable
    @SerialName("edge")
    @Description("Where an edge of the Scene's bounds is (of the screen while the Scene has none, as the edge Event decides), plus offset, as it is when the Block runs; an Actor added from this one still goes to the edge itself.")
    public data class Edge(
        @Description("Which edge: left or right for an x, top or bottom for a y.")
        @SerialName("edge") val edge: SceneEdge = SceneEdge.LEFT,
        @Description("Added to where the edge is: positive is right or down, negative left or up.")
        @SerialName("offset") val offset: Float = 0f,
    ) : NumberValue

    @Serializable
    @SerialName("betweenEdges")
    @Description("A random number between two edges of the Scene's bounds (of the screen while the Scene has none), each plus its offset, drawn anew each time the Block runs.")
    public data class BetweenEdges(
        @Description("The edge one end is at: left or right for an x, top or bottom for a y, as the to edge.")
        @SerialName("from") val from: SceneEdge = SceneEdge.LEFT,
        @Description("Added to where the from edge is: positive is right or down, negative left or up.")
        @SerialName("fromOffset") val fromOffset: Float = 0f,
        @Description("The edge the other end is at, on the same axis as the from edge.")
        @SerialName("to") val to: SceneEdge = SceneEdge.RIGHT,
        @Description("Added to where the to edge is: positive is right or down, negative left or up.")
        @SerialName("toOffset") val toOffset: Float = 0f,
    ) : NumberValue

    public companion object {

        /**
         * How a schema that doesn't list the kinds of numbers names them, after the field's own description.
         */
        internal const val SUMMARY = "A number, or a random, property, edge, or betweenEdges number value (get_block_catalog's numberValue describes them)."
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
        is NumberValue.Edge -> offset.isFinite()
        is NumberValue.BetweenEdges -> fromOffset.isFinite() && toOffset.isFinite()
    }

/**
 * Whether [this] value reads where the edges of the Scene are.
 */
public val NumberValue.readsEdges: Boolean
    get() = this is NumberValue.Edge || this is NumberValue.BetweenEdges

/**
 * Whether [this] value reads an edge that isn't a position on [axis], as the top edge in an x position.
 */
public fun NumberValue.readsEdgeAcross(axis: Axis): Boolean = when (this) {
    is NumberValue.Edge -> edge.axis != axis
    is NumberValue.BetweenEdges -> from.axis != axis || to.axis != axis
    is NumberValue.Fixed, is NumberValue.Random, is NumberValue.Property -> false
}

/**
 * Whether the x and y of a position or a velocity read only edges of their own axes.
 */
public fun areEdgesAlong(x: NumberValue, y: NumberValue): Boolean = !x.readsEdgeAcross(Axis.HORIZONTAL) && !y.readsEdgeAcross(Axis.VERTICAL)

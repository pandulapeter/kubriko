/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.Axis
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.SceneEdge
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * What a number field of a Block holds, as its menu offers the choice.
 */
internal enum class NumberValueKind {
    FIXED,
    RANDOM,
    RANDOM_WHOLE,
    PROPERTY,
    EDGE,
    BETWEEN_EDGES,
    ;

    /**
     * [value] turned into this kind, keeping what it can: a fixed number becomes a random one between 0 and it, a random
     * one becomes the number it starts from, rounded in a field that only takes whole numbers, an edge becomes one end of
     * a random number between it and the edge across, and the end it starts from becomes an edge again. A new edge starts
     * at the left or top edge, whichever [axis] the field is a position on (the left one when it is neither).
     */
    fun of(value: NumberValue, isWholeField: Boolean = false, axis: Axis? = null): NumberValue = when (this) {
        FIXED -> when (value) {
            is NumberValue.Fixed -> value
            is NumberValue.Random -> NumberValue.Fixed(if (isWholeField) value.from.roundToInt().toFloat() else value.from)
            is NumberValue.Property, is NumberValue.Edge, is NumberValue.BetweenEdges -> NumberValue.Fixed(0f)
        }
        RANDOM, RANDOM_WHOLE -> when (value) {
            is NumberValue.Fixed -> if (value.value == 0f) {
                NumberValue.Random(from = 0f, to = 1f, isWhole = this == RANDOM_WHOLE)
            } else {
                NumberValue.Random(from = min(0f, value.value), to = max(0f, value.value), isWhole = this == RANDOM_WHOLE)
            }
            is NumberValue.Random -> value.copy(isWhole = this == RANDOM_WHOLE)
            is NumberValue.Property, is NumberValue.Edge, is NumberValue.BetweenEdges -> NumberValue.Random(isWhole = this == RANDOM_WHOLE)
        }
        PROPERTY -> value as? NumberValue.Property ?: NumberValue.Property()
        EDGE -> when (value) {
            is NumberValue.Edge -> value
            is NumberValue.BetweenEdges -> NumberValue.Edge(value.from, value.fromOffset)
            is NumberValue.Fixed, is NumberValue.Random, is NumberValue.Property -> NumberValue.Edge(SceneEdge.firstOf(axis ?: Axis.HORIZONTAL))
        }
        BETWEEN_EDGES -> when (value) {
            is NumberValue.BetweenEdges -> value
            is NumberValue.Edge -> NumberValue.BetweenEdges(from = value.edge, fromOffset = value.offset, to = value.edge.opposite)
            is NumberValue.Fixed, is NumberValue.Random, is NumberValue.Property -> SceneEdge.firstOf(axis ?: Axis.HORIZONTAL).let { NumberValue.BetweenEdges(from = it, to = it.opposite) }
        }
    }

    companion object {

        /**
         * The kinds a field offers: one that only takes whole numbers draws whole numbers anyway, so it offers one random
         * kind, and counts things rather than placing them, so it offers no edges, and neither does one that is no
         * position (a direction or a speed); one that already holds an edge still shows it. One read on every frame (the
         * speed of Move with keys) offers no random kind, since it would change on every frame.
         */
        fun offeredBy(isWholeField: Boolean, offersEdges: Boolean = true, offersRandom: Boolean = true) = when {
            isWholeField -> listOf(FIXED, RANDOM, PROPERTY)
            offersEdges -> entries
            else -> listOf(FIXED, RANDOM, RANDOM_WHOLE, PROPERTY)
        }.filter { offersRandom || (it != RANDOM && it != RANDOM_WHOLE) }

        fun of(value: NumberValue, isWholeField: Boolean) = when (value) {
            is NumberValue.Fixed -> FIXED
            is NumberValue.Random -> if (value.isWhole && !isWholeField) RANDOM_WHOLE else RANDOM
            is NumberValue.Property -> PROPERTY
            is NumberValue.Edge -> EDGE
            is NumberValue.BetweenEdges -> BETWEEN_EDGES
        }
    }
}

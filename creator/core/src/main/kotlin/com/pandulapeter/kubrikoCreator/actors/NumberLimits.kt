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
 * The numbers a number field of a Block takes, from [minimum] up to [maximum]: what the field in the editor lets through,
 * what a new kind of number chosen for it starts as, and what a command checks. [minimum] is never above [maximum].
 */
public data class NumberLimits(
    val minimum: Float = Float.NEGATIVE_INFINITY,
    val maximum: Float = Float.POSITIVE_INFINITY,
) {

    init {
        require(minimum <= maximum) { "The minimum ($minimum) is above the maximum ($maximum)." }
    }

    /**
     * Whether the field takes [number].
     */
    public operator fun contains(number: Float): Boolean = number in minimum..maximum

    /**
     * Whether the field takes [value]: a fixed number, or both ends of a random one, within the limits. A Property's value
     * or an edge can't be checked before the game runs, so the generated code keeps it within them instead.
     */
    public operator fun contains(value: NumberValue): Boolean = when (value) {
        is NumberValue.Fixed -> value.value in this
        is NumberValue.Random -> value.from in this && value.to in this
        is NumberValue.Property, is NumberValue.Edge, is NumberValue.BetweenEdges -> true
    }

    /**
     * The number the field takes that is closest to [number].
     */
    public fun clamp(number: Float): Float = number.coerceIn(minimum, maximum)

    public companion object {

        /**
         * Any number.
         */
        public val ANY: NumberLimits = NumberLimits()

        /**
         * A count, a time, a speed of an animation, a strength, or a scale: 0 or more.
         */
        public val NON_NEGATIVE: NumberLimits = NumberLimits(minimum = 0f)

        /**
         * An opacity or a chance: from 0 to 100 percent.
         */
        public val PERCENT: NumberLimits = NumberLimits(minimum = 0f, maximum = 100f)

        /**
         * A volume: from 0 to [FULL_VOLUME] percent.
         */
        public val VOLUME: NumberLimits = NumberLimits(minimum = 0f, maximum = FULL_VOLUME)

        /**
         * A zoom: at least [MINIMUM_ZOOM] percent.
         */
        public val ZOOM: NumberLimits = NumberLimits(minimum = MINIMUM_ZOOM)
    }
}

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
 * What an Animate Block takes from a start to an end, and its two ends.
 */
@Serializable
@Description("What an animate Block changes; type says which.")
public sealed interface AnimatedValue {

    /**
     * Which of the Actor's values it changes, so that an Animate Block of the same one replaces it while it runs.
     */
    public val target: AnimationTarget

    @Serializable
    @SerialName("scale")
    @Description("The scale, both ways: 1 is its own size, 0 or more.")
    public data class Scale(
        @Description("Not an edge.")
        @SerialName("from") val from: NumberValue = NumberValue.Fixed(1f),
        @Description("Not an edge.")
        @SerialName("to") val to: NumberValue = NumberValue.Fixed(2f),
    ) : AnimatedValue {
        override val target: AnimationTarget get() = AnimationTarget.SCALE
    }

    @Serializable
    @SerialName("opacity")
    @Description("The opacity, in percent: 100 is opaque.")
    public data class Opacity(
        @Description("0 to 100. Not an edge.")
        @SerialName("from") val from: NumberValue = NumberValue.Fixed(100f),
        @Description("0 to 100. Not an edge.")
        @SerialName("to") val to: NumberValue = NumberValue.Fixed(0f),
    ) : AnimatedValue {
        override val target: AnimationTarget get() = AnimationTarget.OPACITY
    }

    @Serializable
    @SerialName("color")
    @Description("The color, keeping the opacity.")
    public data class Color(
        @Description("#RRGGBB.")
        @Serializable(with = HexColorSerializer::class)
        @SerialName("from") val from: Long = 0xFFFFFFFF,
        @Description("#RRGGBB.")
        @Serializable(with = HexColorSerializer::class)
        @SerialName("to") val to: Long = 0xFFE05050,
    ) : AnimatedValue {
        override val target: AnimationTarget get() = AnimationTarget.COLOR
    }

    @Serializable
    @SerialName("position")
    @Description("The position, from where it is; it adds up with the velocity.")
    public data class Position(
        @Description("Where it ends, or how far it moves.")
        @SerialName("x") val x: NumberValue = NumberValue.Fixed(0f),
        @Description("Where it ends, or how far it moves down.")
        @SerialName("y") val y: NumberValue = NumberValue.Fixed(-100f),
        @Description("True to move by x and y, false to move to them. An edge is the Scene's either way.")
        @SerialName("isRelative") val isRelative: Boolean = true,
    ) : AnimatedValue {
        override val target: AnimationTarget get() = AnimationTarget.POSITION
    }
}

/**
 * The values of an Actor an Animate Block can change, each with one animation of its own at a time.
 *
 * @param fieldPrefix What the names of the members the generated class keeps the animation in start with.
 */
public enum class AnimationTarget(internal val fieldPrefix: String) {
    SCALE("scale"),
    OPACITY("opacity"),
    COLOR("color"),
    POSITION("position"),
}

/**
 * How an animation goes from its start to its end.
 */
@Serializable
public enum class AnimationEasing {
    @SerialName("linear")
    LINEAR,

    @SerialName("easeIn")
    EASE_IN,

    @SerialName("easeOut")
    EASE_OUT,

    @SerialName("easeInOut")
    EASE_IN_OUT,
}

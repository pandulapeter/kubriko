/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.types

import com.pandulapeter.kubriko.extensions.rad
import kotlin.jvm.JvmInline
import kotlin.math.PI

/**
 * Wrapper for values that represent angles in radians.
 *
 * Use the [rad] extension property for casting.
 * Use the [normalized] property to always get a value between 0 and 2π.
 */
@JvmInline
value class AngleRadians internal constructor(private val raw: Float) : Comparable<AngleRadians> {
    val normalized: Float
        get() = (raw % TwoPi.raw + TwoPi.raw) % TwoPi.raw

    operator fun plus(other: AngleRadians): AngleRadians = (raw + other.raw).rad

    operator fun unaryPlus(): AngleRadians = raw.unaryPlus().rad

    operator fun minus(other: AngleRadians): AngleRadians = (raw - other.raw).rad

    operator fun unaryMinus(): AngleRadians = raw.unaryMinus().rad

    operator fun times(scale: Float): AngleRadians = (raw * scale).rad

    operator fun times(scale: Int): AngleRadians = (raw * scale).rad

    operator fun div(scale: Float): AngleRadians = (raw / scale).rad

    operator fun div(scale: Int): AngleRadians = (raw / scale).rad

    override operator fun compareTo(other: AngleRadians) = raw.compareTo(other.raw)

    override fun toString(): String = "AngleRadians($raw)"

    companion object {
        val Zero = 0f.rad
        val Pi = PI.toFloat().rad
        val TwoPi = Pi * 2
    }
}

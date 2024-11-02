package com.pandulapeter.kubriko.engine.types

import com.pandulapeter.kubriko.engine.implementation.extensions.deg
import kotlin.jvm.JvmInline

/**
 * Wrapper for values that represent angles in degrees.
 *
 * Use the [deg] extension property for casting.
 * Use the [normalized] property to always get a value between 0 and 360.
 */
@JvmInline
value class AngleDegrees internal constructor(val raw: Float) : Comparable<AngleDegrees> {
    val normalized: Float
        get() = (raw % 360 + 360) % 360

    operator fun plus(other: AngleDegrees): AngleDegrees = (raw + other.raw).deg

    operator fun unaryPlus(): AngleDegrees = raw.unaryPlus().deg

    operator fun minus(other: AngleDegrees): AngleDegrees = (raw - other.raw).deg

    operator fun unaryMinus(): AngleDegrees = raw.unaryMinus().deg

    operator fun times(scale: Float): AngleDegrees = (raw * scale).deg

    operator fun times(scale: Int): AngleDegrees = (raw * scale).deg

    operator fun div(scale: Float): AngleDegrees = (raw / scale).deg

    operator fun div(scale: Int): AngleDegrees = (raw / scale).deg

    override operator fun compareTo(other: AngleDegrees) = raw.compareTo(other.raw)

    operator fun rangeTo(other: AngleDegrees) = raw.rangeTo(other.raw)

    operator fun rangeUntil(other: AngleDegrees) = raw.rangeUntil(other.raw)

    override fun toString(): String = "AngleDegrees($raw°)"
}

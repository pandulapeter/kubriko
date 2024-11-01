package com.pandulapeter.kubriko.engine.types

import kotlin.jvm.JvmInline

@JvmInline
value class AngleDegrees(val raw: Float) {
    val normalized: Float
        get() = (raw % 360 + 360) % 360

    operator fun plus(other: AngleDegrees): AngleDegrees =
        AngleDegrees(raw + other.raw)

    operator fun minus(other: AngleDegrees): AngleDegrees =
        AngleDegrees(raw - other.raw)

    operator fun times(scale: Float): AngleDegrees =
        AngleDegrees(raw * scale)

    operator fun times(scale: Int): AngleDegrees =
        AngleDegrees(raw * scale)

    operator fun div(scale: Float): AngleDegrees =
        AngleDegrees(raw / scale)

    operator fun div(scale: Int): AngleDegrees =
        AngleDegrees(raw / scale)

    override fun toString(): String = "angleDegrees=$raw°"
}

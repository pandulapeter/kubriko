package com.pandulapeter.kubriko.engine.types

import com.pandulapeter.kubriko.engine.implementation.extensions.rad
import kotlin.jvm.JvmInline
import kotlin.math.PI


@JvmInline
value class AngleRadians(private val raw: Float) {
    val normalized: Float
        get() = (raw % TwoPi.raw + TwoPi.raw) % TwoPi.raw

    operator fun plus(other: AngleRadians): AngleRadians =
        AngleRadians(raw + other.raw)

    operator fun minus(other: AngleRadians): AngleRadians =
        AngleRadians(raw - other.raw)

    operator fun times(scale: Float): AngleRadians =
        AngleRadians(raw * scale)

    operator fun times(scale: Int): AngleRadians =
        AngleRadians(raw * scale)

    operator fun div(scale: Float): AngleRadians =
        AngleRadians(raw / scale)

    operator fun div(scale: Int): AngleRadians =
        AngleRadians(raw / scale)

    override fun toString(): String = "angleRadians=$raw°"

    companion object {
        val Zero = 0f.rad
        val Pi = PI.toFloat().rad
        val TwoPi = Pi * 2
    }
}

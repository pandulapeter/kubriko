package com.pandulapeter.gameTemplate.engine.types

import kotlin.jvm.JvmInline

@JvmInline
value class AngleDegrees(private val degrees: Float) {
    val normalized: Float
        get() = (degrees % 360 + 360) % 360

    operator fun plus(other: AngleDegrees): AngleDegrees =
        AngleDegrees(normalized + other.normalized)

    operator fun minus(other: AngleDegrees): AngleDegrees =
        AngleDegrees(normalized - other.normalized)

    override fun toString(): String = "rotationDegrees=$normalized°"
}

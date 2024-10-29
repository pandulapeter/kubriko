package com.pandulapeter.gameTemplate.engine.types

import kotlin.jvm.JvmInline

@JvmInline
value class RotationDegrees(private val degrees: Float) {
    val normalized: Float
        get() = (degrees % 360 + 360) % 360

    operator fun plus(other: RotationDegrees): RotationDegrees =
        RotationDegrees(normalized + other.normalized)

    operator fun minus(other: RotationDegrees): RotationDegrees =
        RotationDegrees(normalized - other.normalized)

    override fun toString(): String = "rotationDegrees=$normalized°"
}

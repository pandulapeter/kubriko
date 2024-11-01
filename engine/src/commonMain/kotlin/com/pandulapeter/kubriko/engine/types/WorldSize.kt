package com.pandulapeter.kubriko.engine.types

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import kotlin.jvm.JvmInline

@JvmInline
value class WorldSize(val rawSize: Size) {
    val width: Float get() = rawSize.width
    val height: Float get() = rawSize.height
    val center get() = WorldCoordinates(rawSize.center)

    constructor(width: Float, height: Float) : this(Size(width, height))

    operator fun plus(other: WorldSize): WorldSize = WorldSize(
        width = width + other.width,
        height = height + other.height,
    )

    operator fun minus(other: WorldSize): WorldSize = WorldSize(
        width = width - other.width,
        height = height - other.height,
    )

    operator fun times(scale: Float): WorldSize = WorldSize(rawSize * scale)

    operator fun times(scale: Int): WorldSize = WorldSize(rawSize * scale.toFloat())

    operator fun div(scale: Float): WorldSize = WorldSize(rawSize / scale)

    operator fun div(scale: Int): WorldSize = WorldSize(rawSize / scale.toFloat())

    override fun toString(): String = "MapSize(width=$width, height=$height)"

    companion object {
        val Zero = WorldSize(Size.Zero)
    }
}
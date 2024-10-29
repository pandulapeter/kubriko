package com.pandulapeter.gameTemplate.engine.types

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import kotlin.jvm.JvmInline

@JvmInline
value class MapSize(val rawSize: Size) {
    val width: Float get() = rawSize.width
    val height: Float get() = rawSize.height
    val center get() = MapCoordinates(rawSize.center)

    constructor(width: Float, height: Float) : this(Size(width, height))

    operator fun plus(other: MapSize): MapSize = MapSize(
        width = width + other.width,
        height = height + other.height,
    )

    operator fun minus(other: MapSize): MapSize = MapSize(
        width = width - other.width,
        height = height - other.height,
    )

    operator fun times(scale: Float): MapSize = MapSize(rawSize * scale)

    operator fun div(scale: Float): MapSize = MapSize(rawSize / scale)

    override fun toString(): String = "MapSize(width=$width, height=$height)"

    companion object {
        val Zero = MapSize(Size.Zero)
    }
}
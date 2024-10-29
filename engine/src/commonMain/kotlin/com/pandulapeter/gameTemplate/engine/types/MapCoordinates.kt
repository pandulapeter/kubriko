package com.pandulapeter.gameTemplate.engine.types

import androidx.compose.ui.geometry.Offset
import kotlin.jvm.JvmInline

@JvmInline
value class MapCoordinates(val rawOffset: Offset) {
    val x: Float get() = rawOffset.x
    val y: Float get() = rawOffset.y

    constructor(x: Float, y: Float) : this(Offset(x, y))

    operator fun plus(other: MapCoordinates): MapCoordinates = MapCoordinates(rawOffset + other.rawOffset)

    operator fun minus(other: MapCoordinates): MapCoordinates = MapCoordinates(rawOffset - other.rawOffset)

    operator fun times(scale: Float): MapCoordinates = MapCoordinates(rawOffset * scale)

    operator fun div(scale: Float): MapCoordinates = MapCoordinates(rawOffset / scale)

    override fun toString(): String = "MapCoordinates(x=$x, y=$y)"

    companion object {
        val Zero = MapCoordinates(Offset.Zero)
    }
}
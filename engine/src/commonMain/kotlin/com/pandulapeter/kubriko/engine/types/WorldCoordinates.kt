package com.pandulapeter.kubriko.engine.types

import androidx.compose.ui.geometry.Offset
import kotlin.jvm.JvmInline

@JvmInline
value class WorldCoordinates(val rawOffset: Offset) {
    val x: Float get() = rawOffset.x
    val y: Float get() = rawOffset.y

    constructor(x: Float, y: Float) : this(Offset(x, y))

    operator fun plus(other: WorldCoordinates): WorldCoordinates = WorldCoordinates(rawOffset + other.rawOffset)

    operator fun minus(other: WorldCoordinates): WorldCoordinates = WorldCoordinates(rawOffset - other.rawOffset)

    operator fun times(scale: Float): WorldCoordinates = WorldCoordinates(rawOffset * scale)

    operator fun div(scale: Float): WorldCoordinates = WorldCoordinates(rawOffset / scale)

    override fun toString(): String = "MapCoordinates(x=$x, y=$y)"

    companion object {
        val Zero = WorldCoordinates(Offset.Zero)
    }
}
package com.pandulapeter.gameTemplate.engine.types

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import kotlin.jvm.JvmInline

@JvmInline
value class Scale(private val size: Size) {
    val horizontal: Float get() = size.width
    val vertical: Float get() = size.height
    val center get() = WorldCoordinates(size.center)

    constructor(horizontal: Float, vertical: Float) : this(Size(horizontal, vertical))

    operator fun plus(other: Scale): Scale = Scale(
        horizontal = horizontal + other.horizontal,
        vertical = vertical + other.vertical,
    )

    operator fun minus(other: Scale): Scale = Scale(
        horizontal = horizontal - other.horizontal,
        vertical = vertical - other.vertical,
    )

    operator fun times(scale: Float): Scale = Scale(size * scale)

    operator fun div(scale: Float): Scale = Scale(size / scale)

    override fun toString(): String = "Scale(horizontal=$horizontal, vertical=$vertical)"

    companion object {
        val Zero = Scale(Size.Zero)
        val Unit = Scale(1f, 1f)
    }
}
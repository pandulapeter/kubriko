package com.pandulapeter.kubriko.engine.types

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue

/**
 * Note: Negative scale is not supported.
 */
@JvmInline
value class Scale(private val size: Size) {
    val horizontal: Float get() = size.width.absoluteValue
    val vertical: Float get() = size.height.absoluteValue
    val center get() = WorldCoordinates(Size(horizontal, vertical).center)

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
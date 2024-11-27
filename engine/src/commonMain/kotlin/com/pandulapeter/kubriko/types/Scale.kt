package com.pandulapeter.kubriko.types

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import kotlin.jvm.JvmInline

/**
 * 2D scaling factor.
 */
@JvmInline
value class Scale(private val raw: Size) {
    val horizontal: Float get() = raw.width
    val vertical: Float get() = raw.height
    val center get() = SceneOffset(Size(horizontal, vertical).center)

    constructor(horizontal: Float, vertical: Float) : this(Size(horizontal, vertical))

    operator fun plus(other: Scale): Scale = Scale(
        horizontal = horizontal + other.horizontal,
        vertical = vertical + other.vertical,
    )

    operator fun minus(other: Scale): Scale = Scale(
        horizontal = horizontal - other.horizontal,
        vertical = vertical - other.vertical,
    )

    operator fun times(scale: Float): Scale = Scale(raw * scale)

    operator fun times(scale: Int): Scale = Scale(raw * scale.toFloat())

    operator fun div(scale: Float): Scale = Scale(raw / scale)

    operator fun div(scale: Int): Scale = Scale(raw / scale.toFloat())

    override fun toString(): String = "Scale(horizontal=$horizontal, vertical=$vertical)"

    companion object {
        val Zero = Scale(Size.Zero)
        val Unit = Scale(1f, 1f)
    }
}
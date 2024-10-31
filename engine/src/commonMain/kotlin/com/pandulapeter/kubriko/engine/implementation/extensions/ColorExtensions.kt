package com.pandulapeter.kubriko.engine.implementation.extensions

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

fun Color.toHSV(): Triple<Float, Float, Float> {
    val r = red
    val g = green
    val b = blue
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta) % 6
        max == g -> ((b - r) / delta) + 2
        else -> ((r - g) / delta) + 4
    } * 60
    return Triple(if (hue < 0) hue + 360 else hue, if (max == 0f) 0f else delta / max, max)
}
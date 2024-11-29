package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.types.ScenePixel
import kotlin.math.max
import kotlin.math.min

fun ScenePixel.clamp(
    min: ScenePixel? = null,
    max: ScenePixel? = null,
) = max(min?.raw ?: raw, min(max?.raw ?: raw, raw)).scenePixel
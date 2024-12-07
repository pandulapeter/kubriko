package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.max
import kotlin.math.min

fun SceneUnit.clamp(
    min: SceneUnit? = null,
    max: SceneUnit? = null,
) = max(min?.raw ?: raw, min(max?.raw ?: raw, raw)).sceneUnit
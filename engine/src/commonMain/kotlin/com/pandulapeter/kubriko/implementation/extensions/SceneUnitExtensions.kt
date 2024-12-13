package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun SceneUnit.clamp(
    min: SceneUnit? = null,
    max: SceneUnit? = null,
) = max(min?.raw ?: raw, min(max?.raw ?: raw, raw)).sceneUnit

val SceneUnit.abs get() = abs(raw).sceneUnit

fun min(a: SceneUnit, b: SceneUnit) = min(a.raw, b.raw).sceneUnit

fun max(a: SceneUnit, b: SceneUnit) = min(a.raw, b.raw).sceneUnit
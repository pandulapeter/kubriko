package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.types.AngleDegrees
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit

val Float.deg: AngleDegrees
    get() = AngleDegrees(this)

val Float.rad: AngleRadians
    get() = AngleRadians(this)

val Float.sceneUnit: SceneUnit
    get() = SceneUnit(this)

operator fun Float.times(scale: SceneUnit): SceneUnit = (scale.raw * this).sceneUnit
package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.types.AngleDegrees
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit

val Int.deg: AngleDegrees
    get() = AngleDegrees(toFloat())

val Int.rad: AngleRadians
    get() = AngleRadians(toFloat())

val Int.sceneUnit: SceneUnit
    get() = SceneUnit(toFloat())
package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.types.AngleDegrees
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.ScenePixel

val Int.deg: AngleDegrees
    get() = AngleDegrees(toFloat())

val Int.rad: AngleRadians
    get() = AngleRadians(toFloat())

val Int.scenePixel: ScenePixel
    get() = ScenePixel(toFloat())
package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.types.AngleDegrees
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.ScenePixel

val Float.deg: AngleDegrees
    get() = AngleDegrees(this)

val Float.rad: AngleRadians
    get() = AngleRadians(this)

val Float.scenePixel: ScenePixel
    get() = ScenePixel(this)
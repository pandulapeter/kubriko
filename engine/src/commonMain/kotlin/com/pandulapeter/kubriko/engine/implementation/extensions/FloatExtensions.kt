package com.pandulapeter.kubriko.engine.implementation.extensions

import com.pandulapeter.kubriko.engine.types.AngleDegrees
import com.pandulapeter.kubriko.engine.types.AngleRadians
import com.pandulapeter.kubriko.engine.types.ScenePixel

val Float.deg: AngleDegrees
    get() = AngleDegrees(this)

val Float.rad: AngleRadians
    get() = AngleRadians(this)

val Float.scenePixel: ScenePixel
    get() = ScenePixel(this)
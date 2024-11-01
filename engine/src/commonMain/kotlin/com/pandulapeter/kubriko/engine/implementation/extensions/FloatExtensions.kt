package com.pandulapeter.kubriko.engine.implementation.extensions

import com.pandulapeter.kubriko.engine.types.AngleDegrees
import com.pandulapeter.kubriko.engine.types.AngleRadians

val Float.deg: AngleDegrees
    get() = AngleDegrees(this)

val Float.rad: AngleRadians
    get() = AngleRadians(this)
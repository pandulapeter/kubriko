package com.pandulapeter.kubriko.engine.implementation.extensions

import com.pandulapeter.kubriko.engine.types.WorldCoordinates
import kotlin.math.atan2

fun WorldCoordinates.angleTowards(position: WorldCoordinates) = atan2(position.y - y, position.x - x).radiansToDegrees()
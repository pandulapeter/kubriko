package com.pandulapeter.gameTemplate.engine.implementation.extensions

import com.pandulapeter.gameTemplate.engine.types.MapCoordinates
import kotlin.math.atan2

fun MapCoordinates.angleTowards(position: MapCoordinates) = atan2(position.y - y, position.x - x).radiansToDegrees()
package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.math.atan2

fun SceneOffset.angleTowards(position: SceneOffset): AngleRadians = atan2((position.y - y).raw, (position.x - x).raw).rad
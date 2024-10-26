package com.pandulapeter.gameTemplate.engine.implementation.extensions

import kotlin.math.PI

fun Float.toDegrees() = this * 180f / PI.toFloat()

fun Float.toRadians() = this * (PI / 180f).toFloat()
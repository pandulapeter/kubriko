/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.body.PointBody
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Where an Actor is and how it is drawn, as the Scene editor's inspector shows it: read from the Actor itself, so the
 * values its state leaves out (and its class gives defaults to) show too.
 *
 * @param size The width and height, or null for a body without a size.
 */
internal data class ActorBodyValues(
    val x: Float,
    val y: Float,
    val size: Pair<Float, Float>? = null,
    val rotationInDegrees: Float = 0f,
    val scaleHorizontal: Float = 1f,
    val scaleVertical: Float = 1f,
) {
    companion object {
        fun of(body: PointBody) = if (body is BoxBody) {
            ActorBodyValues(
                x = body.position.x.raw,
                y = body.position.y.raw,
                size = body.size.width.raw to body.size.height.raw,
                rotationInDegrees = degreesOf(body.rotation.normalized),
                scaleHorizontal = body.scale.horizontal,
                scaleVertical = body.scale.vertical,
            )
        } else {
            ActorBodyValues(body.position.x.raw, body.position.y.raw)
        }

        /**
         * Rounded to a thousandth, so that an angle typed in degrees reads back the same after its round trip through radians.
         */
        internal fun degreesOf(radians: Float) = (radians * 180 / PI * 1000).roundToInt() / 1000f
    }
}

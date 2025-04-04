/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers.extensions

import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset

fun PointBody.transformForViewport(drawTransform: DrawTransform) {
    val pivot = if (this is BoxBody) pivot else SceneOffset.Zero
    drawTransform.translate(
        left = (position.x - pivot.x).raw,
        top = (position.y - pivot.y).raw,
    )
    if (this is BoxBody) {
        if (rotation != AngleRadians.Zero) {
            drawTransform.rotate(
                degrees = rotation.deg.normalized,
                pivot = pivot.raw,
            )
        }
        if (scale != Scale.Unit) {
            drawTransform.scale(
                scaleX = scale.horizontal,
                scaleY = scale.vertical,
                pivot = pivot.raw,
            )
        }
    }
}
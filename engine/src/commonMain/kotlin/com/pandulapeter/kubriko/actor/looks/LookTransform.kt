/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.looks

import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians

/**
 * The affine map from a body's own coordinates to the scene's, `x' = a·x + c·y + tx`, `y' = b·x + d·y + ty`: the same
 * translation, rotation around the pivot, and scale around the pivot that `transformForViewport` applies to the
 * canvas for a plain `Visible`, done on the CPU instead.
 */
internal class LookTransform {
    var a = 1f
        private set
    var b = 0f
        private set
    var c = 0f
        private set
    var d = 1f
        private set
    var tx = 0f
        private set
    var ty = 0f
        private set

    fun setToIdentity() {
        a = 1f
        b = 0f
        c = 0f
        d = 1f
        tx = 0f
        ty = 0f
    }

    fun setToBody(body: BoxBody) {
        val rotation = body.rotation
        val cos = if (rotation == AngleRadians.Zero) 1f else rotation.cos
        val sin = if (rotation == AngleRadians.Zero) 0f else rotation.sin
        val scaleX = body.scale.horizontal
        val scaleY = body.scale.vertical
        a = cos * scaleX
        b = sin * scaleX
        c = -sin * scaleY
        d = cos * scaleY
        val pivotX = body.pivot.x.raw
        val pivotY = body.pivot.y.raw
        tx = body.position.x.raw - (a * pivotX + c * pivotY)
        ty = body.position.y.raw - (b * pivotX + d * pivotY)
    }

    fun x(localX: Float, localY: Float) = a * localX + c * localY + tx

    fun y(localX: Float, localY: Float) = b * localX + d * localY + ty
}

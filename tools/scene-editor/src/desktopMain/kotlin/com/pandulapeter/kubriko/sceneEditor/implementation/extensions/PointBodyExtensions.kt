/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.extensions

import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.collision.mask.PointCollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.center
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.SceneOffset

internal val PointBody.boundingBoxCollisionMask
    get() = when (this) {
        is BoxBody -> renderedCorners().let { corners ->
            PolygonCollisionMask(
                vertices = corners,
                initialPosition = corners.center,
            )
        }

        else -> PointCollisionMask(initialPosition = position)
    }

/**
 * The four corners of the box in scene space, mirroring how [BoxBody] scales around its pivot, rotates,
 * and translates itself when drawn, so that click picking lines up with the rendered shape regardless of
 * the body's scale, rotation, or pivot.
 */
private fun BoxBody.renderedCorners(): List<SceneOffset> {
    val pivotX = pivot.x.raw
    val pivotY = pivot.y.raw
    val scaledLeft = -pivotX * scale.horizontal
    val scaledTop = -pivotY * scale.vertical
    val scaledRight = (size.width.raw - pivotX) * scale.horizontal
    val scaledBottom = (size.height.raw - pivotY) * scale.vertical
    val cos = rotation.cos
    val sin = rotation.sin
    val positionX = position.x.raw
    val positionY = position.y.raw
    fun corner(localX: Float, localY: Float) = SceneOffset(
        x = (localX * cos - localY * sin + positionX).sceneUnit,
        y = (localX * sin + localY * cos + positionY).sceneUnit,
    )
    return listOf(
        corner(scaledLeft, scaledTop),
        corner(scaledRight, scaledTop),
        corner(scaledRight, scaledBottom),
        corner(scaledLeft, scaledBottom),
    )
}
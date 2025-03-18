/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.physics.implementation.geometry.Polygon
import com.pandulapeter.kubriko.types.SceneOffset

internal class DynamicPolygon(
    initialOffset: SceneOffset,
    shape: Polygon,
) : BaseDynamicObject() {
    override val collisionMask = PolygonCollisionMask(
        vertices = shape.vertices.map { SceneOffset(it.x, it.y) },
        initialPosition = initialOffset,
    )
    override val body = BoxBody(
        initialPosition = initialOffset,
        initialSize = collisionMask.size
    )
    override val shouldClip = false // TODO: This shouldn't be needed, there must be an issue with the AABB calculation
    override val physicsBody = PhysicsBody(
        shape = shape,
        x = initialOffset.x,
        y = initialOffset.y,
    ).apply {
        restitution = 0.4f
    }

    override fun DrawScope.draw() {
        val path = Path().apply {
            moveTo(collisionMask.vertices[0].x.raw + body.pivot.x.raw, collisionMask.vertices[0].y.raw + body.pivot.y.raw)
            for (i in 1 until collisionMask.vertices.size) {
                lineTo(collisionMask.vertices[i].x.raw + body.pivot.x.raw, collisionMask.vertices[i].y.raw + body.pivot.y.raw)
            }
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Fill,
        )
        drawPath(
            path = path,
            color = Color.Black,
            style = Stroke(width = 2f),
        )
    }
}

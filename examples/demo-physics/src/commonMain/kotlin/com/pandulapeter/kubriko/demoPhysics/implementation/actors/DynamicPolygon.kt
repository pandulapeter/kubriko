/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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

internal class DynamicPolygon(
    override val collisionMask: PolygonCollisionMask,
) : BaseDynamicObject() {
    override val body = BoxBody(
        initialPosition = collisionMask.position,
        initialSize = collisionMask.size,
    )
    override val physicsBody = PhysicsBody(collisionMask).apply {
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

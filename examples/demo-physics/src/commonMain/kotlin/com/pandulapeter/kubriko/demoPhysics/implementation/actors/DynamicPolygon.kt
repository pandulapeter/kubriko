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
import com.pandulapeter.kubriko.actor.body.PolygonBody
import com.pandulapeter.kubriko.physics.implementation.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.geometry.Polygon
import com.pandulapeter.kubriko.types.SceneOffset

// TODO: Expose to the editor
internal class DynamicPolygon(
    initialOffset: SceneOffset,
    shape: Polygon,
) : BaseDynamicObject() {
    override val body = PolygonBody(
        initialPosition = initialOffset,
        vertices = shape.vertices.map { SceneOffset(it.x, it.y) },
    )
    override val physicsBody = Body(
        shape = shape,
        x = initialOffset.x,
        y = initialOffset.y,
    ).apply {
        restitution = 1f
    }
    // TODO: Hacky workaround for incorrect math
    override val shouldClip = false

    override fun DrawScope.draw() {
        val path = Path().apply {
            moveTo(body.vertices[0].x.raw + body.pivot.x.raw, body.vertices[0].y.raw + body.pivot.y.raw)
            for (i in 1 until body.vertices.size) {
                lineTo(body.vertices[i].x.raw + body.pivot.x.raw, body.vertices[i].y.raw + body.pivot.y.raw)
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

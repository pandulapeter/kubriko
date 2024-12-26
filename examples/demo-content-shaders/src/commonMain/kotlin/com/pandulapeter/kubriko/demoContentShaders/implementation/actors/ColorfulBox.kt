package com.pandulapeter.kubriko.demoContentShaders.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class ColorfulBox(
    initialPosition: SceneOffset,
    private var hue: Float,
) : Visible, Dynamic {

    override val body = RectangleBody(
        initialPosition = initialPosition,
        initialSize = SceneSize(width = 100.sceneUnit, height = 100.sceneUnit),
    )

    override fun update(deltaTimeInMillis: Float) {
        hue += deltaTimeInMillis / 10f
        while (hue > 360f) {
            hue -= 360f
        }
    }

    override fun DrawScope.draw() = drawRect(
        color = Color.hsv(hue, 0.5f, 1f),
        size = body.size.raw,
    )
}
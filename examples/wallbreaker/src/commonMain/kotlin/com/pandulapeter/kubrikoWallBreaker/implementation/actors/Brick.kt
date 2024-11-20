package com.pandulapeter.kubrikoWallbreaker.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class Brick(
    override var position: SceneOffset,
    val hue: Float,
) : Visible {
    override val boundingBox: SceneSize = SceneSize(Width, Height)
    private val color = Color.hsv(hue, 0.2f, 0.9f)

    override fun draw(scope: DrawScope) {
        scope.drawRect(
            color = color,
            size = boundingBox.raw,
        )
        scope.drawRect(
            color = Color.Black,
            size = boundingBox.raw,
            style = Stroke(),
        )
    }

    companion object {
        val Width = 100f.scenePixel
        val Height = 40f.scenePixel
    }
}

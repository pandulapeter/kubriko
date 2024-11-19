package com.pandulapeter.kubrikoShowcase.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.constrainedWithin
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize

class Ball(
    size: ScenePixel = 20f.scenePixel,
    speed: ScenePixel = 0.5f.scenePixel,
) : Visible, Dynamic {

    override val canvasIndex = -1
    override val boundingBox: SceneSize = SceneSize(size, size)
    override var position: SceneOffset = SceneOffset.Zero
    private var speedX = speed
    private var speedY = speed
    private lateinit var viewportManager: ViewportManager

    override fun onAdd(kubriko: Kubriko) {
        viewportManager = kubriko.require()
    }

    override fun update(deltaTimeInMillis: Float) {
        val viewportTopLeft = viewportManager.topLeft.value
        val viewportBottomRight = viewportManager.bottomRight.value
        position = position.constrainedWithin(viewportTopLeft, viewportBottomRight)
        val nextPosition = position + SceneOffset(speedX, speedY) * deltaTimeInMillis
        if (nextPosition.x < viewportTopLeft.x || nextPosition.x > viewportBottomRight.x) {
            speedX *= -1
        }
        if (nextPosition.y < viewportTopLeft.y || nextPosition.y > viewportBottomRight.y) {
            speedY *= -1
        }
        position = nextPosition
    }

    override fun draw(scope: DrawScope) = scope.drawCircle(
        color = Color.LightGray,
        radius = (boundingBox.raw.width + boundingBox.raw.height) / 2,
        center = pivotOffset.raw,
    )
}

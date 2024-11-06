package com.pandulapeter.kubrikoPong.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize

class Ball(
    private val size: ScenePixel = 20f.scenePixel,
    speed: ScenePixel = 1.5f.scenePixel,
) : Visible, Dynamic {

    override val boundingBox: SceneSize = SceneSize(size, size)
    override var position: SceneOffset = SceneOffset.Zero
    private val boxColor: Color = Color.Gray
    private var speedX = speed
    private var speedY = speed
    private lateinit var viewportManager: ViewportManager

    override fun onAdd(kubriko: Kubriko) {
        viewportManager = kubriko.require()
    }

    override fun update(deltaTimeInMillis: Float) {
        val viewportCenter = viewportManager.cameraPosition.value
        val viewportSize = viewportManager.size.value
        val viewportScaleFactor = viewportManager.scaleFactor.value
        val viewportTopLeft = Offset.Zero.toSceneOffset(
            viewportCenter = viewportCenter,
            viewportSize = viewportSize,
            viewportScaleFactor = viewportScaleFactor,
        )
        val viewportBottomRight = Offset(viewportSize.width, viewportSize.height).toSceneOffset(
            viewportCenter = viewportCenter,
            viewportSize = viewportSize,
            viewportScaleFactor = viewportScaleFactor,
        )
        // Validate current position to handle potential bugs caused by resizing the screen
        if (position.x < viewportTopLeft.x) {
            position = SceneOffset(viewportTopLeft.x + size, position.y)
        }
        if (position.x > viewportBottomRight.x) {
            position = SceneOffset(viewportBottomRight.x - size, position.y)
        }
        if (position.y < viewportTopLeft.y) {
            position = SceneOffset(position.x, viewportTopLeft.y + size)
        }
        if (position.y > viewportBottomRight.y) {
            position = SceneOffset(position.x, viewportBottomRight.y - size)
        }
        // Validate the next position, bounce if needed
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
        color = boxColor,
        radius = (boundingBox.raw.width + boundingBox.raw.height) / 2,
        center = pivotOffset.raw,
    )
}

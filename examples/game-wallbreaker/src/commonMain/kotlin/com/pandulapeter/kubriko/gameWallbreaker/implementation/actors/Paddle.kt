package com.pandulapeter.kubriko.gameWallbreaker.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class Paddle(
    initialPosition: SceneOffset = SceneOffset(0.sceneUnit, 550.sceneUnit),
) : Visible, Collidable, PointerInputAware {

    override val body = RectangleBody(
        initialPosition = initialPosition,
        initialSize = SceneSize(Width, Height),
    )

    private lateinit var stateManager: StateManager
    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        stateManager = kubriko.require()
        viewportManager = kubriko.require()
    }

    override fun DrawScope.draw() {
        drawRect(
            color = Color.White,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            size = body.size.raw,
            style = Stroke(),
        )
    }

    override fun onPointerOffsetChanged(screenOffset: Offset) {
        if (stateManager.isRunning.value) {
            body.position = SceneOffset(
                x = screenOffset.toSceneOffset(viewportManager).x,
                y = body.position.y,
            )
        }
    }

    companion object {
        val Width = 200f.sceneUnit
        val Height = 40f.sceneUnit
    }
}

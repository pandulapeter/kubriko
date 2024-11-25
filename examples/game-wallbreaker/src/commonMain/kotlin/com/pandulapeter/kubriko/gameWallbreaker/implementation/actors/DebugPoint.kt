package com.pandulapeter.kubriko.gameWallbreaker.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class DebugPoint(
    override var position: SceneOffset,
    hue: Float,
) : Visible {
    override val boundingBox: SceneSize = SceneSize(Brick.Width, Brick.Height)
    private val color = Color.hsv(hue, 0.2f, 0.9f)
    override var scale: Scale = Scale.Unit
    private lateinit var actorManager: ActorManager
    private var alpha = 1f


    override fun DrawScope.draw() {
        drawRect(
            color = color.copy(alpha = alpha),
            size = boundingBox.raw,
        )
        drawRect(
            color = Color.Black.copy(alpha = alpha),
            size = boundingBox.raw,
            style = Stroke(),
        )
    }
}

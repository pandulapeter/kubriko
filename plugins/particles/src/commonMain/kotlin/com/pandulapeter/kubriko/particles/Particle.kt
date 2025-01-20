package com.pandulapeter.kubriko.particles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.types.AngleRadians

abstract class Particle(
    override val body: RectangleBody,
    val speed: Float,
    val direction: AngleRadians,
) : Visible, Dynamic {

    private lateinit var actorManager: ActorManager

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Float) {
        // TODO: Move, remove when needed
    }

    override fun DrawScope.draw() {
        with(body) {
            drawDebugBounds(color = Color.Blue, stroke = Stroke())
        }
    }
}
package com.pandulapeter.kubriko.particles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.cos
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.sin
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

class Particle(
    override val body: RectangleBody,
    override val drawingOrder: Float = 0f,
    private val speed: SceneUnit = SceneUnit.Zero,
    private val direction: AngleRadians = AngleRadians.Zero,
    private val lifespanInMilliseconds: Float,
) : Visible, Dynamic {

    private lateinit var actorManager: ActorManager
    private var remainingLifespan = lifespanInMilliseconds

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Float) {
        // TODO: Move, apply transformations
        if (speed != SceneUnit.Zero) {
            body.position = SceneOffset(
                x = body.position.x + speed * direction.cos,
                y = body.position.y - speed * direction.sin,
            )
        }
        remainingLifespan -= deltaTimeInMilliseconds
        if (remainingLifespan <= 0) {
            actorManager.remove(this)
        }
    }

    override fun DrawScope.draw() = drawRect(
        color = Color.Red.copy(alpha = remainingLifespan / lifespanInMilliseconds),
        size = size,
        style = Fill,
    )
}